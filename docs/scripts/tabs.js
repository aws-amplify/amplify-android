function openLang(evt, lang) {
    // Declare all variables
    var i, tabcontent, tablinks;

    coord = getCoords(evt.target);
    elementYOffset = coord.top - window.scrollY;

    // Get all elements with class="tabcontent" and hide them
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        if (tabcontent[i].classList.contains(lang)) {
            tabcontent[i].style.display = "block";
        } else {
            tabcontent[i].style.display = "none";
        }
    }

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
        if (tablinks[i].classList.contains(lang)) {
            tablinks[i].className += " active";
        }
    }

    coord = getCoords(evt.target);
    scrolll = coord.top - elementYOffset;
    window.scroll(0, scrolll);
}
function getCoords(elem) { // crossbrowser version
    var box = elem.getBoundingClientRect();

    var body = document.body;
    var docEl = document.documentElement;

    var scrollTop = window.pageYOffset || docEl.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docEl.scrollLeft || body.scrollLeft;

    var clientTop = docEl.clientTop || body.clientTop || 0;
    var clientLeft = docEl.clientLeft || body.clientLeft || 0;

    var top  = box.top +  scrollTop - clientTop;
    var left = box.left + scrollLeft - clientLeft;

    return { top: Math.round(top), left: Math.round(left) };
}

tablinks = document.getElementsByClassName("tablinks defaultOpen");
if (tablinks.length > 0) {
    tablinks[0].click();
}
window.scrollTo(0,0);