/* 质检记录页 */
let pg = 1, productNameMap = {};

async function ensureProductNames() {
  if (Object.keys(productNameMap).length) return;
  const j = await api('/api/products');
  if (j.code === 0) j.data.forEach(p => productNameMap[p.id] = p.name);
}

async function loadRecords(page) {
  pg = Math.max(1, page);
  await ensureProductNames();
  const q = new URLSearchParams({
    pageNum: pg, pageSize: 10,
    productId: document.getElementById('fProduct').value,
    batchNo: document.getElementById('fBatch').value.trim(),
    overallResult: document.getElementById('fResult').value,
    status: document.getElementById('fStatus').value
  });
  const j = await api('/api/records?' + q);
  if (j.code !== 0) { toast(j.msg); return; }
  const d = j.data;
  document.getElementById('recordRows').innerHTML = d.records.map(r => `
    <tr>
      <td>#${r.id}</td><td>${productNameMap[r.productId] || r.productId}</td><td>${r.batchNo}</td>
      <td>${r.overallResult === 'PASS' ? '<span class="badge pass">PASS</span>' : '<span class="badge fail">FAIL</span>'}</td>
      <td>${r.defectCount}</td><td><span class="badge mock">${r.source}</span></td>
      <td>${r.status === 'CONFIRMED' ? '<span class="badge done">已复核</span>' : '<span class="badge wait">待复核</span>'}</td>
      <td>${(r.createTime || '').replace('T', ' ').slice(0, 19)}</td>
      <td><button class="btn ghost sm" onclick="openDetail(${r.id})">详情</button></td>
    </tr>`).join('') || '<tr><td colspan="9" style="text-align:center;color:var(--muted)">暂无记录</td></tr>';
  document.getElementById('pgInfo').textContent = `第 ${d.current} / ${d.pages || 1} 页，共 ${d.total} 条`;
}

async function openDetail(id) {
  const j = await api('/api/records/' + id);
  if (j.code !== 0) { toast(j.msg); return; }
  const r = j.data;
  await ensureProductNames();
  const result = JSON.parse(r.aiResultJson || '{}');
  document.getElementById('detailKv').innerHTML = `
    <div class="k">产品</div><div>${productNameMap[r.productId] || ''}</div>
    <div class="k">批次</div><div>${r.batchNo}</div>
    <div class="k">AI结论</div><div>${r.overallResult}（缺陷 ${r.defectCount} 个，来源 ${r.source}，耗时 ${r.latencyMs}ms）</div>
    <div class="k">人工复核</div><div>${r.status === 'CONFIRMED' ? `${r.humanVerdict} · ${r.inspector || ''} · ${r.humanNote || ''}` : '待复核'}</div>
    <div class="k">检测时间</div><div>${(r.createTime || '').replace('T', ' ')}</div>`;
  const img = document.getElementById('detailImg');
  img.onload = () => drawBboxes(document.getElementById('detailCanvas'), img, result.defects || []);
  img.src = '/' + r.imagePath;
  document.getElementById('detailJson').textContent = JSON.stringify(result, null, 2);
  document.getElementById('detailMask').classList.add('show');
}

function closeDetail() { document.getElementById('detailMask').classList.remove('show'); }
