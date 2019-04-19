completions = JSON.parse(sessions);
let elementsArray = document.querySelectorAll(".completion");
document.addEventListener("click", function (e) {
    if (!(e.target.className === "completion")) {
        closeAllLists(e.target);
    }
});
elementsArray.forEach(function(elem) {
    elem.addEventListener("click", function() {
        arr = completions[elem.id];
        var a, b, i, val = this.value;
        closeAllLists();
        a = document.createElement("DIV");
        a.setAttribute("id", this.id + "autocomplete-list");
        a.setAttribute("class", "autocomplete-items");
        this.appendChild(a);
        for (i = 0; i < arr.length; i++) {
            b = document.createElement("DIV");
            if (elem.firstChild.data == arr[i]) {
                b.innerHTML = "<b>" + arr[i] + "</b>"
            } else {
                b.innerHTML = arr[i]
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