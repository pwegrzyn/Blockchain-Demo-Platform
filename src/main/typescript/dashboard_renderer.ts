
let isConnected: boolean = false;

function update_dashboard() {
    if (!isConnected) {
        document.getElementById("mainContent").style.display = "none";
        document.getElementById("loadingIcon").style.display = "block";
    } else {
        document.getElementById("mainContent").style.display = "block";
        document.getElementById("loadingIcon").style.display = "none";
    } 
}

update_dashboard()
setInterval(update_dashboard, 1000)