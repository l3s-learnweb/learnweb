/* eslint-disable no-await-in-loop,no-restricted-syntax,import/no-extraneous-dependencies */
import puppeteer from 'puppeteer';
import percySnapshot from '@percy/puppeteer';

const rootUrl = 'https://learnweb.l3s.uni-hannover.de/dev';

const offlineScreenshot = true;
const offlinePath = './target/screenshots';

const percyScreenshot = !!process.env.PERCY_SERVER_ADDRESS;
const percyWidths = [360, 768, 1366];

const publicPages = [
  { url: '/lw/', name: '001_homepage' },
  { url: '/lw/contact.jsf', name: '002_contact' },
  { url: '/lw/imprint.jsf', name: '003_imprint' },
  { url: '/lw/privacy_statement.jsf', name: '004_privacy_statement' },
  { url: '/lw/user/register.jsf', name: '005_register' },
  { url: '/lw/user/login.jsf', name: '006_login' },
  { url: '/lw/user/password.jsf', name: '007_forgot_password' },
];

const privatePages = [
  { url: '/lw/', name: '101_homepage_authenticated' },

  { url: '/lw/myhome/welcome.jsf', name: '201_welcome' },
  { url: '/lw/user/detail.jsf', name: '202_user_detail' },
  { url: '/lw/myhome/profile.jsf', name: '203_user_profile' },
  { url: '/lw/myhome/notification.jsf', name: '204_notification' },
  { url: '/lw/myhome/resources.jsf', name: '205_my_resources' },
  { url: '/lw/myhome/comments.jsf', name: '206_my_comments' },
  { url: '/lw/myhome/tags.jsf', name: '207_my_tags' },
  { url: '/lw/myhome/rated_resources.jsf', name: '208_my_rates' },
  { url: '/lw/myhome/groups.jsf', name: '209_my_groups' },
  { url: '/lw/myhome/groups.jsf', name: '210_new_group', script: "PF('createGroupDialog').show();" },
  { url: '/lw/myhome/groups_search.jsf', name: '211_groups_search' },

  { url: '/lw/group/overview.jsf?group_id=7', name: '301_group_overview' },
  { url: '/lw/group/overview.jsf?group_id=4', name: '302_group_overview_not_member' },
  { url: '/lw/group/resources.jsf?group_id=7', name: '302_group_resources' },
  { url: '/lw/group/resources.jsf?group_id=4', name: '302_group_resources_not_member' },
  { url: '/lw/group/resources.jsf?group_id=7&folder_id=32', name: '303_group_resources_subfolder' },
  { url: '/lw/group/forum.jsf?group_id=10', name: '304_group_forum' },
  { url: '/lw/group/forum_topic.jsf?topic_id=6', name: '305_group_forum_topic' },
  { url: '/lw/group/forum_topic.jsf?topic_id=6', name: '306_group_forum_topic_reply', script: "PF('postDialog').show();" },
  { url: '/lw/group/members.jsf?group_id=7', name: '307_group_members' },
  { url: '/lw/group/options.jsf?group_id=7', name: '308_group_options' },
  { url: '/lw/group/options.jsf?group_id=1', name: '309_group_options_leader' },

  { url: '/lw/search.jsf?query=Apollo+11&action=text', name: '401_search' },
  { url: '/lw/search.jsf?query=Apollo+11&action=image', name: '402_search_images' },
  { url: '/lw/search.jsf?query=Apollo+11&action=image', name: '403_search_single_image', script: "document.querySelector('#resource_4').click();" },
  { url: '/lw/search.jsf?query=Apollo+11&action=video', name: '404_search_videos' },
  { url: '/lw/search.jsf?query=Apollo+11&action=video', name: '405_search_single_video', script: "document.querySelector('#resource_5').click();" },

  { url: '/lw/resource.jsf?resource_id=514', name: '501_resource_image' },
  { url: '/lw/resource.jsf?resource_id=481', name: '502_resource_video' },
  { url: '/lw/resource.jsf?resource_id=532', name: '503_resource_text' },
  { url: '/lw/resource.jsf?resource_id=531', name: '504_resource_audio' },
  { url: '/lw/resource.jsf?resource_id=480', name: '505_resource_website' },
  { url: '/lw/resource.jsf?resource_id=15', name: '506_resource_spreadsheet' },
  { url: '/lw/resource.jsf?resource_id=483', name: '507_resource_document' },
  { url: '/lw/resource.jsf?resource_id=530', name: '508_resource_presentation' },
  { url: '/lw/resource.jsf?resource_id=426', name: '509_resource_pdf' },
  { url: '/lw/resource.jsf?resource_id=533', name: '510_resource_file' },
  { url: '/lw/resource.jsf?resource_id=303', name: '511_resource_glossary' },
  { url: '/lw/resource.jsf?resource_id=59', name: '512_resource_survey' },
  { url: '/lw/survey/survey.jsf?resource_id=59', name: '513_resource_survey_open' },

  { url: '/lw/dashboard/index.jsf', name: '601_dashboard_index' },
  {
    url: '/lw/dashboard/glossary.jsf',
    name: '602_glossary_dashboard',
    script: "$('#select_users_form\\\\:dateStart_input').val('01.01.2021'); $('#select_users_form\\\\:dateEnd_input').val('01.01.2022'); $('#select_users_form').submit();",
  },
  {
    url: '/lw/dashboard/activity.jsf',
    name: '603_activity_dashboard',
    script: "$('#select_users_form\\\\:dateStart_input').val('01.01.2021'); $('#select_users_form\\\\:dateEnd_input').val('01.01.2022'); $('#select_users_form').submit();",
  },
  {
    url: '/lw/dashboard/tracker.jsf',
    name: '604_tracker_dashboard',
    script: "$('#select_users_form\\\\:dateStart_input').val('01.01.2021'); $('#select_users_form\\\\:dateEnd_input').val('01.01.2022'); $('#select_users_form').submit();",
  },
  { url: '/lw/myhome/search_history.jsf', name: '651_search_history' },

  { url: '/lw/your_information/index.jsf', name: '801_your_information_index' },
  { url: '/lw/your_information/your_personal_info.jsf', name: '802_your_information_personal_info' },
  { url: '/lw/your_information/your_groups.jsf', name: '803_your_information_your_groups' },
  // remaining pages in this folder are very similar to "your groups" hence need no further testing
];

// Before executing, set PERCY_TOKEN env variable
(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    ignoreHTTPSErrors: true,
    defaultViewport: {
      width: 1920,
      height: 1080,
    },
  });

  const page = await browser.newPage();

  // Take screenshot of all public pages
  for (const p of publicPages) {
    await takeScreenshot(page, rootUrl + p.url, p);
  }

  // Login to the system
  await page.goto(`${rootUrl}/lw/user/login.jsf`, { waitUntil: 'load' });
  await page.type('#login_form\\:username', 'astappiev');
  await page.type('#login_form\\:password', 'astappiev');
  await page.click('#login_form\\:login_button');
  await page.waitForNavigation();

  // Take screenshot of private pages
  for (const p of privatePages) {
    await takeScreenshot(page, rootUrl + p.url, p);
  }

  await browser.close();
})();

async function takeScreenshot(page, url, options) {
  console.log(`Navigating to ${url}`);
  await page.goto(url, { waitUntil: 'networkidle2' });
  await timeout(page);

  if (options.script) {
    console.log('Executing page script...');
    await page.evaluate(options.script);
    await timeout(page, 30000);
  }

  console.log(`Taking snapshot of ${options.name}`);
  if (offlineScreenshot) {
    await page.screenshot({ path: `${offlinePath}/${options.name}.png`, fullPage: true });
  }
  if (percyScreenshot) {
    await percySnapshot(page, options.name, { widths: percyWidths });
  }
}

async function timeout(page, ms = 10000) {
  try {
    await page.waitForNetworkIdle({ timeout: ms });
  } catch (ignored) {
    console.log('Timeout waiting for network idle');
  }
}
