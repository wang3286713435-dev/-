
let gridPixelPoints = null;
let polygonPoints2D = null;

self.onmessage = async (e) => {
  const { type, id, position, result, error } = e.data;

  if (type === 'init') {
    gridPixelPoints = e.data.gridPixelPoints;
    polygonPoints2D = e.data.polygonPoints2D;

    if (!Array.isArray(gridPixelPoints) || !gridPixelPoints.length || !Array.isArray(polygonPoints2D) || !polygonPoints2D.length) {
      self.postMessage({
        type: 'error',
        error: 'Invalid input: gridPixelPoints or polygonPoints2D is empty or not an array'
      });
      return;
    }

    processPoints();
    return;
  }

  if (type === 'pickupResult' || type === 'sceneToGisCoordResult') {
    if (pendingRequests.has(id)) {
      const { resolve, reject } = pendingRequests.get(id);
      if (error) {
        reject(new Error(error));
      } else {
        resolve(result);
      }
      pendingRequests.delete(id);
    }
  }
};

const pendingRequests = new Map();
let requestId = 0;

async function pickupCoordinate({ position }) {
  return new Promise((resolve, reject) => {
    const id = requestId++;
    pendingRequests.set(id, { resolve, reject });
    self.postMessage({ type: 'pickup', id, position });
  });
}

async function sceneToGisCoord({ position }) {
  return new Promise((resolve, reject) => {
    const id = requestId++;
    pendingRequests.set(id, { resolve, reject });
    self.postMessage({ type: 'sceneToGisCoord', id, position });
  });
}

async function processPoints() {
  const heights = [];
  const worldPoints = [];
  let maxHeight = -Infinity;
  let minHeight = Infinity;

  try {
    for (let i = 0; i < gridPixelPoints.length; i++) {
      const pickResult = await pickupCoordinate({
        position: gridPixelPoints[i]
      });
      if (pickResult && pickResult.position && Array.isArray(pickResult.position) && pickResult.position.length >= 3) {
        if (isPointInPolygon({ x: pickResult.position[0], y: pickResult.position[1] }, polygonPoints2D)) {
          const wgs84Pos = await sceneToGisCoord({
            position: pickResult.position
          });
          if (wgs84Pos && Array.isArray(wgs84Pos) && wgs84Pos.length >= 3) {
            heights.push(wgs84Pos[2]);
            maxHeight = Math.max(maxHeight, wgs84Pos[2]);
            minHeight = Math.min(minHeight, wgs84Pos[2]);
            worldPoints.push([pickResult.position[0], pickResult.position[1], pickResult.position[2]]);
          }
        }
      }
    }
    self.postMessage({
      type: 'result',
      heights,
      worldPoints,
      maxHeight: heights.length > 0 ? maxHeight : -Infinity,
      minHeight: heights.length > 0 ? minHeight : Infinity
    });
  } catch (error) {
    self.postMessage({ type: 'error', error: error.message });
  }
}

function isPointInPolygon(point, polygon) {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].x, yi = polygon[i].y;
    const xj = polygon[j].x, yj = polygon[j].y;
    const intersect = ((yi > point.y) !== (yj > point.y)) &&
      (point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi);
    if (intersect) inside = !inside;
  }
  return inside;
}
