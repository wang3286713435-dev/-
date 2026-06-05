function buildBatchTexture(payload) {
  const names = payload.names || [];
  const colors = payload.colors || [];
  const featuresLength = payload.featuresLength || 0;
  const nWidth = payload.nWidth || 1;
  const nHeight = payload.nHeight || 1;
  const byteLength = nWidth * nHeight * 4;
  const bytes = new Uint8Array(byteLength);
  const blending = new Uint8Array(byteLength);
  const flow = payload.openFlow ? new Uint8Array(byteLength) : null;
  const breath = payload.openBreath ? new Uint8Array(byteLength) : null;
  const lodColor = payload.lodColor || [255, 255, 255];
  const lodVisible = payload.lodVisible !== false;
  const lodAlpha = Number.isFinite(payload.lodAlpha) ? payload.lodAlpha : 1;
  const alphaBase = lodVisible ? Math.max(0, Math.min(255, Math.round(lodAlpha * 255))) : 0;
  const partSet = new Set(Array.isArray(payload.partLoadFeatureIds) ? payload.partLoadFeatureIds : []);
  const styleByTag = payload.styleByTag || {};
  const styleByName = payload.styleByName || {};
  const existingByName = payload.existingFirstByName || {};
  const isInstanced = !!payload.isInstanced;
  const instancedCounts = Array.isArray(payload.instancedCounts) ? payload.instancedCounts : [];
  const groupChildrenLength = payload.groupChildrenLength || 0;
  const entries = [];
  let cIndex = 0;
  let fIndex = 0;

  for (let i = 0; i < featuresLength; i++) {
    const name = names[i];
    const offset = i * 4;
    let r = lodColor[0], g = lodColor[1], b = lodColor[2];
    if (colors.length === featuresLength) {
      const col = colors[i] >>> 0;
      r = (col >> 16) & 255;
      g = (col >> 8) & 255;
      b = col & 255;
    }
    let a = alphaBase;
    if (partSet.size > 0) {
      a = partSet.has(name) ? a : 0;
    }

    const st = styleByTag[name] || styleByName[name];
    if (st) {
      if (st.color) {
        r = st.color[0];
        g = st.color[1];
        b = st.color[2];
      }
      if (typeof st.visible === "boolean") {
        a = st.visible ? 255 : 0;
      }
    }

    const ex = existingByName[name];
    if (ex) {
      if (ex.color) {
        r = ex.color[0];
        g = ex.color[1];
        b = ex.color[2];
      }
      if (typeof ex.visible === "boolean") {
        a = ex.visible ? 255 : 0;
      }
    }

    bytes[offset] = r;
    bytes[offset + 1] = g;
    bytes[offset + 2] = b;
    bytes[offset + 3] = a;

    let meshIndex = 0;
    if (isInstanced) {
      meshIndex = cIndex;
      fIndex++;
      if (instancedCounts[cIndex] != null && fIndex >= instancedCounts[cIndex]) {
        cIndex++;
        fIndex = 0;
      }
    } else {
      meshIndex = groupChildrenLength === featuresLength ? i : 0;
    }

    // 不在 entry 里存 name 字符串，主线程用 names[batchId] 还原，节省 structured-clone 开销
    const entry = { batchId: i, meshIndex };
    if (st && st.color) entry.color = st.color;
    if (st && typeof st.visible === "boolean") entry.visible = st.visible;
    if (ex && ex.color) entry.color = ex.color;
    if (ex && typeof ex.visible === "boolean") entry.visible = ex.visible;
    if (ex && Number.isFinite(ex.alpha) && ex.alpha < 1) entry.alpha = ex.alpha;
    entries.push(entry);
  }

  // updateTexture / directionTexture 在主线程按非副本逻辑生成（_createUpdateBatchTexture / _createbatchIdsDirectionTexture）
  return {
    bytes: bytes.buffer,
    blending: blending.buffer,
    flow: flow ? flow.buffer : null,
    breath: breath ? breath.buffer : null,
    entries,
  };
}

function buildPickTexture(payload) {
  const namesLength = payload.namesLength || 0;
  const maxSize = payload.maxSize || 4096;
  const startKey = payload.startKey || 0;
  const nWidth = Math.min(Math.max(1, namesLength), maxSize);
  const nHeight = Math.ceil(namesLength / nWidth);
  const bytes = new Uint8Array(nWidth * nHeight * 4);
  const keys = new Uint32Array(namesLength);
  for (let i = 0; i < namesLength; i++) {
    const key = startKey + i + 1;
    keys[i] = key >>> 0;
    const offset = i * 4;
    bytes[offset] = key & 0xff;
    bytes[offset + 1] = (key >> 8) & 0xff;
    bytes[offset + 2] = (key >> 16) & 0xff;
    bytes[offset + 3] = (key >> 24) & 0xff;
  }
  return {
    bytes: bytes.buffer,
    keys: keys.buffer,
    nWidth,
    nHeight,
    endKey: startKey + namesLength,
  };
}

self.onmessage = function (e) {
  const d = e.data || {};
  const msgId = d.msgId;
  const payload = d.payload || {};
  const task = payload.type || "batch";
  try {
    if (task === "pick") {
      const result = buildPickTexture(payload);
      const transfers = [result.bytes, result.keys];
      self.postMessage({ msgId, ok: true, result }, transfers);
      return;
    }
    const result = buildBatchTexture(payload);
    const transfers = [result.bytes, result.blending];
    if (result.flow) transfers.push(result.flow);
    if (result.breath) transfers.push(result.breath);
    self.postMessage({ msgId, ok: true, result }, transfers);
  } catch (err) {
    self.postMessage({
      msgId,
      ok: false,
      error: err && err.message ? err.message : String(err),
    });
  }
};
