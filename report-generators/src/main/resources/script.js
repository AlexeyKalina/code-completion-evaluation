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
        var suggests = lookups[lookups.length - 1]["suggests"];
        var a, b, i, val = this.value;
        closeAllLists();
        a = document.createElement("DIV");
        a.setAttribute("id", this.id + "autocomplete-list");
        a.setAttribute("class", "autocomplete-items");
        this.appendChild(a);
        for (i = 0; i < suggests.length; i++) {
            b = document.createElement("DIV");
            var parametersString = completions[elem.id].tokenType === "METHOD_CALL"
                ? '(' + suggests[i].parameters.join(", ") + ')' : "";
            if (completions[elem.id].expectedText === suggests[i].text) {
                b.innerHTML = "<b>" + suggests[i].text + parametersString + "</b>"
            } else {
                b.innerHTML = suggests[i].text + parametersString
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