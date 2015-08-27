function onlinkClick(element) {
	var googleAuthObj = gapi.auth2.getAuthInstance();
	googleAuthObj.signIn().then(function(){window.open(element.href, '_blank')});
	return false;
}

function onSignIn(googleUser) {
  var profile = googleUser.getBasicProfile();
  console.log('ID: ' + profile.getId()); 
  console.log('Name: ' + profile.getName());
  console.log('Image URL: ' + profile.getImageUrl());
  console.log('Email: ' + profile.getEmail());
}

$(document).ready(function(){
	gapi.load('auth2', function() {

	      gapi.auth2.init({fetch_basic_profile: false,
	          scope:'https://www.googleapis.com/auth/plus.login'}).then(
	            function (){
	              console.log('init google sign in');
	            });
	    });


})