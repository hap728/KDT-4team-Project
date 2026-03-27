(() => {
  const loginBtn = document.getElementById("loginBtn");
  const idInput = document.getElementById("loginEmail");
  const pwInput = document.getElementById("loginPassword");
  const githubBtn = document.getElementById("loginGithub");

  const readMessage = async (res, fallback = "요청 처리에 실패했습니다.") => {
    try {
      const data = await res.json();
      return data.message || fallback;
    } catch {
      return fallback;
    }
  };

  const login = async () => {
    const id = idInput?.value?.trim();
    const pw = pwInput?.value?.trim();

    if (!id || !pw) {
      alert("아이디와 비밀번호를 입력해 주세요.");
      return;
    }

    try {
      const res = await fetch("/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ id, pw }),
      });

      if (!res.ok) {
        throw new Error(await readMessage(res, "로그인에 실패했습니다."));
      }

      window.location.href = "/";
    } catch (err) {
      alert(`로그인 실패: ${err.message}`);
    }
  };

  const handleKey = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      login();
    }
  };

  loginBtn?.addEventListener("click", login);
  idInput?.addEventListener("keydown", handleKey);
  pwInput?.addEventListener("keydown", handleKey);

  githubBtn?.addEventListener("click", () => {
    window.location.href = "/oauth2/authorization/github";
  });
})();
