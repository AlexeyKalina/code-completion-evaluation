completions = JSON.parse(sessions);
let elementsArray = document.querySelectorAll(".completion");
document.addEventListener("click", function (e) {
    if (!(e.target.className === "completion")) {
        closeAllLists(e.target);
    }
});
elementsArray.forEach(function(elem) {
    elem.addEventListener("click", function() {
        var lookups = completions[elem.id]["_lookups"];
        var suggestions = lookups[lookups.length - 1]["suggestions"];
        var a, b, i, val = this.value;
        closeAllLists();
        a = document.createElement("DIV");
        a.setAttribute("id", this.id + "autocomplete-list");
        a.setAttribute("class", "autocomplete-items");
        this.appendChild(a);
        for (i = 0; i < suggestions.length; i++) {
            b = document.createElement("DIV");
            if (completions[elem.id].expectedText === suggestions[i].text) {
                b.innerHTML = "<b>" + suggestions[i].presentationText + "</b>"
            } else {
                b.innerHTML = suggestions[i].presentationText
            }
            a.appendChild(b);
        }
    });
});
function closeAllLists(elmnt) {
    var x = document.getElementsByClassName("autocomplete-items");
    for (var i = 0; i < x.length; i++) {
        x[i].parentNode.removeChild(x[i]);
    }
}