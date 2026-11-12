// Build only; the website serves the compiled CSS and does not load a Tailwind CDN script.
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const scope={tailwind:{}};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'components/tutor-layout/tailwind-theme.js'),'utf8'),scope);
module.exports={...scope.tailwind.config,content:[
  path.join(__dirname,'pages/support-ticket/index.html'),path.join(__dirname,'pages/support-tickets/index.html'),
  path.join(__dirname,'components/tutor-layout/tutor-layout.js'),
  path.join(__dirname,'components/learner-layout/learner-layout.js'),
  path.join(__dirname,'components/admin-shell/admin-shell.js'),
  path.join(__dirname,'js/pages/{support-tickets,cs-ticket-detail,tutor-frame,learner-frame}.js'),
  path.join(__dirname,'components/user-menu/user-menu.js'),
  path.join(__dirname,'pages/notifications/index.html'),path.join(__dirname,'js/pages/notifications.js'),
  path.join(__dirname,'js/main.js')
]};
