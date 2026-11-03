import {api,byId,text,date,query,notice,emptyState,createOperationsPage} from '../../js/pages/operations.js';
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const metrics = ['totalAccounts','newAccounts','connectionCount','bookingCount'];
function groups(id,rows) {
  const container=byId(id);container.replaceChildren();
  if(!rows.length){container.append(emptyState('Chưa có dữ liệu trong nhóm này','Số lượng bằng 0 theo dữ liệu hiện có.'));return;}
  const table=text('table');const head=text('thead');const header=text('tr');
  const label=text('th','Nhóm');label.scope='col';const count=text('th','Số lượng');count.scope='col';header.append(label,count);head.append(header);
  const body=text('tbody');rows.forEach(item=>{const row=text('tr');row.append(text('td',item.label),text('td',number(item.count)));body.append(row);});
  table.append(head,body);container.append(table);
}
async function load(){
  try{
    const data=await api('/admin/operations/management-report?'+query(byId('managementFilters')));
    byId('managementFilters').elements.startDate.value=data.startDate;byId('managementFilters').elements.endDate.value=data.endDate;
    byId('managementPeriod').textContent='Kỳ '+data.startDate.split('-').reverse().join('/')+' – '+data.endDate.split('-').reverse().join('/')+' · Cập nhật trạng thái: '+date(data.generatedAt);
    metrics.forEach(id=>{byId(id).textContent=number(data[id]);});
    groups('accountsByRole',data.accountsByRole);groups('accountsByStatus',data.accountsByStatus);groups('connectionGroups',data.connections);groups('bookingGroups',data.bookings);
    byId('acceptanceRate').textContent=data.acceptanceRatePercent==null?'Chưa có yêu cầu':number(data.acceptanceRatePercent)+'%';
    notice('Đã tổng hợp từ dữ liệu tài khoản, kết nối và học thử.');
  }catch(error){
    metrics.forEach(id=>{byId(id).textContent='—';});byId('acceptanceRate').textContent='—';byId('managementPeriod').textContent='Chưa tải được số liệu cho kỳ đã chọn.';
    ['accountsByRole','accountsByStatus','connectionGroups','bookingGroups'].forEach(id=>byId(id).replaceChildren());throw error;
  }
}
const page=createOperationsPage(load);page.bind('managementFilters','submit',load);page.start();
