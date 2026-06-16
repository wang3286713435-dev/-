importScripts('https://cdn.jsdelivr.net/npm/three@0.132.2/build/three.min.js');
const { Ray, Triangle, Vector2, Vector3, Box2 } = THREE;
self.onmessage = function (e) {
  const { points, meshData } = e.data;
  const results = [];

  const ray = new Ray();
  const triangle = new Triangle();
  const v0 = new Vector3();
  const v1 = new Vector3();
  const v2 = new Vector3();
  const intersectPoint = new Vector3();
  const normal = new Vector3();
  const localRayOrigin = new Vector3();

  points.forEach(point => {
    const hitPoint = fastRaycastPoint(point.x, point.y, meshData, ray, triangle, v0, v1, v2, intersectPoint, normal, localRayOrigin);
    results.push({ id: point.id, hitPoint });
  });

  self.postMessage(results);
};

function fastRaycastPoint(x, y, meshData, ray, triangle, v0, v1, v2, intersectPoint, normal, localRayOrigin) {
  const { minZ, maxZ } = getMaxZ(meshData);
  ray.set(new Vector3(x, y, maxZ + 1000), new Vector3(0, 0, -1));
  let minDistance = Infinity;
  const hitPoint = new Vector3();
  let hitFound = false;

  meshData.forEach(mesh => {
    const vertices = new Float32Array(mesh.vertices);
    const indices = mesh.indices ? new Uint32Array(mesh.indices) : null;

    const count = indices ? indices.length : vertices.length / 3;
    for (let i = 0; i < count; i += 3) {
      const a = indices ? indices[i] : i;
      const b = indices ? indices[i + 1] : i + 1;
      const c = indices ? indices[i + 2] : i + 2;

      v0.set(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
      v1.set(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
      v2.set(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);

      if (!pointInTriangle2D(x, y, v0, v1, v2)) continue;

      triangle.set(v0, v1, v2);
      if (ray.intersectTriangle(v0, v1, v2, true, intersectPoint) === null) continue;

      triangle.getNormal(normal);
      if (normal.dot(ray.direction) >= 0) continue;

      const distance = localRayOrigin.distanceTo(intersectPoint);
      if (distance < minDistance) {
        minDistance = distance;
        hitPoint.copy(intersectPoint);
        hitFound = true;
      }
    }
  });

  return hitFound ? hitPoint : new Vector3(x, y, minZ);
}

function pointInTriangle2D(x, y, v0, v1, v2) {
  const p = new Vector2(x, y);
  const a = new Vector2(v0.x, v0.y);
  const b = new Vector2(v1.x, v1.y);
  const c = new Vector2(v2.x, v2.y);

  const v0v1 = b.sub(a);
  const v0v2 = c.sub(a);
  const v0p = p.sub(a);

  const dot00 = v0v2.dot(v0v2);
  const dot01 = v0v2.dot(v0v1);
  const dot02 = v0v2.dot(v0p);
  const dot11 = v0v1.dot(v0v1);
  const dot12 = v0v1.dot(v0p);

  const invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
  const u = (dot11 * dot02 - dot01 * dot12) * invDenom;
  const v = (dot00 * dot12 - dot01 * dot02) * invDenom;

  return u >= 0 && v >= 0 && u + v <= 1;
}

function getMaxZ(meshData) {
  let maxZ = -Infinity;
  let minZ = Infinity;
  meshData.forEach(mesh => {
    const vertices = new Float32Array(mesh.vertices);
    for (let i = 2; i < vertices.length; i += 3) {
      maxZ = Math.max(maxZ, vertices[i]);
      minZ = Math.min(minZ, vertices[i]);
    }
  });
  return { minZ, maxZ };
}