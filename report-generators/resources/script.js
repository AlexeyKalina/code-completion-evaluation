completions = JSON.parse(sessions);
let elementsArray = document.querySelectorAll(".completion");
document.addEventListener("click", function (e) {
    if (!(e.target.classList.contains("completion"))) {
        closeAllLists(e.target);
    }
});
elementsArray.forEach(function(elem) {
    elem.addEventListener("click", function() {
        const sessionId = elem.id.split(" ")[0];
        const lookups = completions[sessionId]["_lookups"];
        const prefixLengthInput = document.getElementById("prefix-length");
        const prefixLength = prefixLengthInput != null ? prefixLengthInput.value : 0;
        if (lookups.length <= prefixLength) return;
        const lookup = lookups[prefixLength];
        const suggestions = lookup["suggestions"];
        let a, b, i = this.value;
        closeAllLists();
        a = document.createElement("DIV");
        a.setAttribute("class", "autocomplete-items");
        this.appendChild(a);
        prefixDiv = document.createElement("DIV");
        prefixDiv.setAttribute("style", "background-color: lightgrey;");
        prefixDiv.innerHTML = "prefix: &quot;" + lookup["text"] + "&quot;; latency: " + lookup["latency"];
        a.appendChild(prefixDiv);
        for (i = 0; i < suggestions.length; i++) {
            b = document.createElement("DIV");
            if (completions[sessionId].expectedText === suggestions[i].text) {
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
function changePrefix() {
    const prefixLengthInput = document.getElementById("prefix-length");
    const prefixLength = prefixLengthInput != null ? prefixLengthInput.value : 0;
    const codeContainers = document.getElementsByClassName("code-container");
    for (let i = 0; i < codeContainers.length; i++) {
        if (prefixLength != i) {
            codeContainers[i].classList.add("prefix-hidden");
        } else {
            codeContainers[i].classList.remove("prefix-hidden");
        }
    }
}