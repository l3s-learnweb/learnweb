function onLinkClick(element) {
	if(googleSignInEnabled)
	{
		var googleAuthObj = gapi.auth2.getAuthInstance();
		googleAuthObj.signIn().then(
				function(success)
				{
					window.open(element.href, '_blank');
					onSignIn(googleAuthObj.currentUser.get());
				}
		);
	}
	else
		return true;
	return false;
}

function onSignIn(googleUser) {
  var profile = googleUser.getBasicProfile();
  saveGmailId([{name:'gmail_id', value:profile.getEmail()}]);
}

$(document).ready(function(){
	if(googleSignInEnabled)
	{
		gapi.load('auth2', function() {

		      gapi.auth2.init(
		    	{
		    		fetch_basic_profile: true,
		    		scope:'https://www.googleapis.com/auth/plus.login'
		    	}).then(
		            function (){
		              console.log('init google sign in');
		            });
		    });
	
	}
});