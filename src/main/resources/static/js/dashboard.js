/* 统计看板页 */
const dark = { textStyle: {color: '#8b98ab'}, splitLine: {lineStyle: {color: '#232d3d'}} };

async function loadDashboard() {
  const j = await api('/api/stats/dashboard?days=14');
  if (j.code !== 0) { toast(j.msg); return; }
  const d = j.data;
  document.getElementById('kTotal').textContent = d.total;
  document.getElementById('kRate').textContent = d.passRate + '%';
  document.getElementById('kRate').className = 'v' + (d.passRate < 80 ? ' bad' : d.passRate < 95 ? ' warn' : '');
  document.getElementById('kPending').textContent = d.pending;
  const defectTotal = (d.defectPareto || []).reduce((s, x) => s + x.count, 0);
  document.getElementById('kDefect').textContent = defectTotal;

  // 趋势
  echarts.init(document.getElementById('chTrend')).setOption({
    ...dark,
    tooltip: {trigger: 'axis'},
    xAxis: {type: 'category', data: d.trend.dates},
    yAxis: {type: 'value', min: 0, max: 100, axisLabel: {formatter: '{value}%'}},
    series: [{data: d.trend.rates, type: 'line', smooth: true, areaStyle: {opacity: .15},
      lineStyle: {color: '#22d3ee'}, itemStyle: {color: '#22d3ee'}}]
  });
  // Pareto
  echarts.init(document.getElementById('chPareto')).setOption({
    ...dark,
    tooltip: {trigger: 'axis'},
    xAxis: {type: 'category', data: (d.defectPareto || []).map(x => typeCn(x.type))},
    yAxis: {type: 'value'},
    series: [{data: (d.defectPareto || []).map(x => x.count), type: 'bar',
      itemStyle: {color: p => p.dataIndex === 0 ? '#f87171' : '#fbbf24'}}]
  });
  // 严重度
  echarts.init(document.getElementById('chSev')).setOption({
    tooltip: {trigger: 'item'},
    series: [{type: 'pie', radius: '60%',
      data: Object.entries(d.severityDist || {}).map(([k, v]) => ({name: sevCn(k), value: v})),
      itemStyle: {color: p => ({'严重': '#f87171', '中等': '#fbbf24', '轻微': '#34d399'})[p.name] || '#8b98ab'},
      label: {color: '#dbe4f0'}}]
  });
  // 产品对比
  echarts.init(document.getElementById('chProd')).setOption({
    ...dark,
    tooltip: {trigger: 'axis'},
    xAxis: {type: 'category', data: (d.byProduct || []).map(x => x.name)},
    yAxis: {type: 'value', min: 0, max: 100, axisLabel: {formatter: '{value}%'}},
    series: [{data: (d.byProduct || []).map(x => x.passRate), type: 'bar', itemStyle: {color: '#34d399'}}]
  });
}
