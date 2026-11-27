import {api,byId,query,notice,createOperationsPage} from '../../js/pages/operations.js';
import {barChart,bindChartTooltip} from '../../js/pages/admin-charts.js';
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const metrics = ['totalAccounts','newAccounts','connectionCount','bookingCount'];
function groups(id,rows){barChart(byId(id),'',rows,'Chưa có dữ liệu trong kỳ đã chọn.');}
async function load(){
  try{
    const data=await api('/admin/operations/management-report?'+query(byId('managementFilters')));
    byId('managementFilters').elements.startDate.value=data.startDate;byId('managementFilters').elements.endDate.value=data.endDate;
    metrics.forEach(id=>{byId(id).textContent=number(data[id]);});
    groups('accountsByRole',data.accountsByRole);groups('accountsByStatus',data.accountsByStatus);groups('connectionGroups',data.connections);groups('bookingGroups',data.bookings);
    byId('acceptanceRate').textContent=data.acceptanceRatePercent==null?'Chưa có yêu cầu':number(data.acceptanceRatePercent)+'%';
    notice('Đã tổng hợp từ dữ liệu tài khoản, kết nối và học thử.');
  }catch(error){
    metrics.forEach(id=>{byId(id).textContent='—';});byId('acceptanceRate').textContent='—';
    ['accountsByRole','accountsByStatus','connectionGroups','bookingGroups'].forEach(id=>byId(id).replaceChildren());throw error;
  }
}
bindChartTooltip();
const page=createOperationsPage(load);page.bind('managementFilters','submit',load);page.start();
