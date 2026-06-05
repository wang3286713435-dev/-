/**
 * 轮廓线几何计算（与 Three.js EdgesGeometry 算法一致，无 Three 依赖，便于 Worker 运行）
 */
const DEG2RAD = Math.PI / 180;

function getNormal(ax, ay, az, bx, by, bz, cx, cy, cz, out) {
	const abx = bx - ax;
	const aby = by - ay;
	const abz = bz - az;
	const acx = cx - ax;
	const acy = cy - ay;
	const acz = cz - az;
	let nx = aby * acz - abz * acy;
	let ny = abz * acx - abx * acz;
	let nz = abx * acy - aby * acx;
	const len = Math.sqrt(nx * nx + ny * ny + nz * nz);
	if (len > 0) {
		nx /= len;
		ny /= len;
		nz /= len;
	}
	out.x = nx;
	out.y = ny;
	out.z = nz;
}

function vecFromPos(pos, vi, out) {
	const i = vi * 3;
	out.x = pos[i];
	out.y = pos[i + 1];
	out.z = pos[i + 2];
}

/**
 * @param {Float32Array} position 线性 xyz
 * @param {Uint16Array|Uint32Array|null} index 三角索引，若非索引几何则为 null
 * @param {number} thresholdAngle 角度阈值，与 EdgesGeometry 一致
 */
function buildEdges(position, index, thresholdAngle) {
	const precisionPoints = 4;
	const precision = Math.pow(10, precisionPoints);
	const thresholdDot = Math.cos(DEG2RAD * thresholdAngle);

	const pos = position;
	const vertCount = pos.length / 3;
	const indexCount = index ? index.length : vertCount;

	const hashes = ["", "", ""];
	const edgeData = Object.create(null);

	const a = { x: 0, y: 0, z: 0 };
	const b = { x: 0, y: 0, z: 0 };
	const c = { x: 0, y: 0, z: 0 };
	const _normal = { x: 0, y: 0, z: 0 };
	const _v0 = { x: 0, y: 0, z: 0 };
	const _v1 = { x: 0, y: 0, z: 0 };

	const vertices = [];
	const vertIndices = [];

	for (let i = 0; i < indexCount; i += 3) {
		let i0;
		let i1;
		let i2;
		if (index) {
			i0 = index[i];
			i1 = index[i + 1];
			i2 = index[i + 2];
		} else {
			i0 = i;
			i1 = i + 1;
			i2 = i + 2;
		}

		if (i0 >= vertCount || i1 >= vertCount || i2 >= vertCount) continue;

		vecFromPos(pos, i0, a);
		vecFromPos(pos, i1, b);
		vecFromPos(pos, i2, c);
		getNormal(a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, _normal);

		hashes[0] = `${Math.round(a.x * precision)},${Math.round(a.y * precision)},${Math.round(a.z * precision)}`;
		hashes[1] = `${Math.round(b.x * precision)},${Math.round(b.y * precision)},${Math.round(b.z * precision)}`;
		hashes[2] = `${Math.round(c.x * precision)},${Math.round(c.y * precision)},${Math.round(c.z * precision)}`;

		if (hashes[0] === hashes[1] || hashes[1] === hashes[2] || hashes[2] === hashes[0]) {
			continue;
		}

		const verts = [a, b, c];
		const inds = [i0, i1, i2];

		for (let j = 0; j < 3; j++) {
			const jNext = (j + 1) % 3;
			const vecHash0 = hashes[j];
			const vecHash1 = hashes[jNext];
			const v0 = verts[j];
			const v1 = verts[jNext];

			const hash = `${vecHash0}_${vecHash1}`;
			const reverseHash = `${vecHash1}_${vecHash0}`;

			if (reverseHash in edgeData && edgeData[reverseHash]) {
				const otherN = edgeData[reverseHash].normal;
				const nd =
					_normal.x * otherN.x + _normal.y * otherN.y + _normal.z * otherN.z;
				if (nd <= thresholdDot) {
					vertices.push(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z);
					vertIndices.push(inds[j], inds[jNext]);
				}
				edgeData[reverseHash] = null;
			} else if (!(hash in edgeData)) {
				edgeData[hash] = {
					index0: inds[j],
					index1: inds[jNext],
					normal: { x: _normal.x, y: _normal.y, z: _normal.z },
				};
			}
		}
	}

	for (const key in edgeData) {
		if (edgeData[key]) {
			const { index0, index1 } = edgeData[key];
			vecFromPos(pos, index0, _v0);
			vecFromPos(pos, index1, _v1);
			vertices.push(_v0.x, _v0.y, _v0.z, _v1.x, _v1.y, _v1.z);
			vertIndices.push(index0, index1);
		}
	}

	return {
		positions: new Float32Array(vertices),
		vertIndices: new Uint32Array(vertIndices)
	};
}

self.onmessage = (e) => {
	const { msgId, jobs } = e.data;
	try {
		const results = [];
		const transfers = [];
		for (let j = 0; j < jobs.length; j++) {
			const job = jobs[j];
			const pos = new Float32Array(job.position, 0, job.positionFloatCount);
			let idx = null;
			if (job.indexByteLength) {
				idx = job.indexUint16
					? new Uint16Array(job.index, 0, job.indexLength)
					: new Uint32Array(job.index, 0, job.indexLength);
			}
			const out = buildEdges(pos, idx, job.thresholdAngle);
			results.push({ id: job.id, position: out.positions, vertIndices: out.vertIndices });
			transfers.push(out.positions.buffer, out.vertIndices.buffer);
		}
		self.postMessage({ msgId, results }, transfers);
	} catch (err) {
		self.postMessage({ msgId, error: err && err.message ? err.message : String(err) });
	}
};
