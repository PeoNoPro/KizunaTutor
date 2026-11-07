import {api,byId,text,notice,createOperationsPage} from '../../js/pages/operations.js';
import {fetchAPI} from '../../js/api.js';
const PATH='/admin/operations/profile';
// Mọi nhân viên nội bộ (không chỉ Admin) lấy mã CSRF ở endpoint riêng của trang hồ sơ.
const save=(path,body)=>fetchAPI(path,{method:'PUT',body,csrfPath:PATH+'/csrf'});
const state={profile:null,saving:false};

async function load(){
  const data=await api(PATH);state.profile=data;
  byId('profileName').textContent=data.fullName;byId('profileRole').textContent=data.roleLabel;byId('profileStatus').textContent=data.statusLabel;
  const avatar=byId('profileAvatar');avatar.replaceChildren();avatar.textContent=data.fullName.trim().split(/\s+/).slice(-2).map(word=>word[0]).join('').toLocaleUpperCase('vi-VN');
  if(data.avatarUrl){try{const url=new URL(data.avatarUrl,location.origin);if(url.protocol==='https:'||(url.origin===location.origin&&url.pathname.startsWith('/assets/'))){const img=document.createElement('img');img.src=url.href;img.alt='';img.referrerPolicy='no-referrer';img.addEventListener('error',()=>img.remove());avatar.append(img);}}catch{/* Dùng chữ viết tắt khi địa chỉ ảnh không hợp lệ. */}}
  const details=byId('profileDetails');details.replaceChildren();
  [['Email',data.email],['Số điện thoại',data.phone||'Chưa cập nhật'],['Vai trò',data.roleLabel]].forEach(([label,value])=>details.append(text('dt',label),text('dd',value)));
}

/** Gửi biểu mẫu trong hộp thoại: lỗi hiện ngay trong hộp thoại, thành công thì đóng và báo nổi. */
async function submit(dialogId,noticeId,action){
  if(state.saving)return;state.saving=true;byId(noticeId).textContent='';
  try{await action();byId(dialogId).close();}
  catch(error){if(error.status===401){location.assign('/login');return;}byId(noticeId).textContent=error.message||'Chưa lưu được. Vui lòng thử lại.';}
  finally{state.saving=false;}
}

const page=createOperationsPage(load);
byId('editProfile').addEventListener('click',()=>{byId('profileNotice').textContent='';byId('profileFullName').value=state.profile?.fullName||'';byId('profilePhone').value=state.profile?.phone||'';byId('profileDialog').showModal();});
byId('openPassword').addEventListener('click',()=>{byId('passwordForm').reset();byId('passwordNotice').textContent='';byId('passwordDialog').showModal();});
document.querySelectorAll('[data-close]').forEach(button=>button.addEventListener('click',()=>{if(!state.saving)button.closest('dialog').close();}));
byId('profileForm').addEventListener('submit',event=>{event.preventDefault();submit('profileDialog','profileNotice',async()=>{
  await save(PATH,{fullName:byId('profileFullName').value.trim(),phone:byId('profilePhone').value.trim()});
  await load();notice('Đã lưu thông tin cá nhân.');
  // Header cũng hiện tên mới.
  const name=document.querySelector('.admin-identity strong');if(name&&state.profile)name.textContent=state.profile.fullName;
});});
byId('passwordForm').addEventListener('submit',event=>{event.preventDefault();
  if(byId('newPassword').value!==byId('confirmPassword').value){byId('passwordNotice').textContent='Mật khẩu nhập lại chưa khớp.';return;}
  submit('passwordDialog','passwordNotice',async()=>{
    await save(PATH+'/password',{currentPassword:byId('currentPassword').value,newPassword:byId('newPassword').value});
    byId('passwordForm').reset();notice('Đã đổi mật khẩu. Lần đăng nhập sau hãy dùng mật khẩu mới.');
  });
});
page.start();
