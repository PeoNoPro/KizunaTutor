import {startSupportPage} from '/js/pages/support-tickets.js';
import {reloadIfInsideLearnerShell} from '/js/pages/learner-frame.js';
if(!reloadIfInsideLearnerShell())startSupportPage(false);
