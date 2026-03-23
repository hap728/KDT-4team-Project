(() => {
  const authArea = document.getElementById("authArea");

  const renderLoggedOut = () => {
    if (!authArea) return;
    authArea.innerHTML = '<a class="auth-btn secondary" href="login.html">로그인</a><a class="auth-btn" href="signup.html">회원가입</a>';
  };

  const renderLoggedIn = () => {
    if (!authArea) return;
    authArea.innerHTML = '<a class="auth-btn" href="profile.html">프로필</a><button id="logoutBtn" class="auth-btn secondary" type="button">로그아웃</button>';
    const logoutBtn = document.getElementById("logoutBtn");
    logoutBtn?.addEventListener("click", async () => {
      try {
        await fetch("/users/logout", { method: "POST" });
      } finally {
        window.location.href = "index.html";
      }
    });
  };

  if (!authArea) return;

  fetch("/users/me")
    .then((res) => res.json())
    .then((data) => {
      if (data?.loggedIn) {
        renderLoggedIn();
      } else {
        renderLoggedOut();
      }
    })
    .catch(() => {
      renderLoggedOut();
    });
})();
