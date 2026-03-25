(() => {
  const loginBtn = document.getElementById("loginBtn");
  const idInput = document.getElementById("loginEmail");
  const pwInput = document.getElementById("loginPassword");

  const login = async () => {
    const id = idInput?.value?.trim();
    const pw = pwInput?.value?.trim();

    if (!id || !pw) {
      alert("아이디와 비밀번호를 입력해주세요.");
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
        const text = await res.text();
        throw new Error(text || "로그인 실패");
      }
      window.location.href = "index.html";
    } catch (err) {
      alert(`로그인 실패: ${err.message}`);
    }
  };

  loginBtn?.addEventListener("click", login);
  const handleKey = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      login();
    }
  };
  idInput?.addEventListener("keydown", handleKey);
  pwInput?.addEventListener("keydown", handleKey);
})();
