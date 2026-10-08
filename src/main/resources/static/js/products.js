/* 产品与标准页 */
async function loadProductsPage() {
  await ensureProductNames();
  document.getElementById('prodRows').innerHTML = Object.entries(productNameMap)
    .map(([id, name]) => `<tr><td>—</td><td>${name}</td><td></td></tr>`).join('');
  // 补全编码规格
  const j = await api('/api/products');
  if (j.code === 0) {
    document.getElementById('prodRows').innerHTML = j.data
      .map(p => `<tr><td>${p.code}</td><td>${p.name}</td><td>${p.spec || ''}</td></tr>`).join('');
  }
  loadStandards();
}

async function loadStandards() {
  const pid = document.getElementById('sProduct').value;
  const j = await api('/api/standards' + (pid ? '?productId=' + pid : ''));
  if (j.code !== 0) { toast(j.msg); return; }
  await ensureProductNames();
  document.getElementById('stdRows').innerHTML = j.data.map(s => {
    let types = [];
    try { types = JSON.parse(s.defectTypes || '[]').map(typeCn).join('、'); } catch (e) { types = s.defectTypes; }
    return `<tr><td>${productNameMap[s.productId] || ''}</td><td>${s.checkItem}</td><td>${types}</td><td>${s.severityRules || ''}</td></tr>`;
  }).join('') || '<tr><td colspan="4" style="text-align:center;color:var(--muted)">暂无标准</td></tr>';
}
