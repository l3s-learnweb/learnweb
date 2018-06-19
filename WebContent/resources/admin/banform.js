function setname(name) {
	formObj = document.getElementById("banformdialog");
	nameObj = formObj.elements.item(0);
	nameObj.value = name;
	radioObj = formObj.elements.item(0);
	radioObj.checked = true;
}