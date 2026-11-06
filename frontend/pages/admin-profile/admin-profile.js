import {api,byId,text,date,notice,createOperationsPage} from '../../js/pages/operations.js';
async function load(){
  const data=await api('/admin/operations/profile');byId('profileName').textContent=data.fullName;byId('profileRole').textContent=data.roleLabel;
  const avatar=byId('profileAvatar');avatar.replaceChildren();avatar.textContent=data.fullName.trim().split(/\s+/).slice(-2).map(word=>word[0]).join('').toLocaleUpperCase('vi-VN');
  if(data.avatarUrl){try{const url=new URL(data.avatarUrl,location.origin);if(url.protocol==='https:'||(url.origin===location.origin&&url.pathname.startsWith('/assets/'))){const img=document.createElement('img');img.src=url.href;img.alt='';img.referrerPolicy='no-referrer';img.addEventListener('error',()=>img.remove());avatar.append(img);}}catch{/* Dùng chữ viết tắt khi địa chỉ ảnh không hợp lệ. */}}
  const details=byId('profileDetails');details.replaceChildren();
  [['Họ và tên',data.fullName],['Email',data.email],['Số điện thoại',data.phone||'Chưa cập nhật'],['Vai trò',data.roleLabel],['Trạng thái',data.statusLabel],['Ngày tạo tài khoản',date(data.createdAt)]].forEach(([label,value])=>details.append(text('dt',label),text('dd',value)));
  notice('Đã tải hồ sơ tài khoản đang sử dụng.');
}
createOperationsPage(load).start();
