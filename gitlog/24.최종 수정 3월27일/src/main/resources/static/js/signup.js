(() => {
  const signupBtn = document.getElementById("signupBtn");
  const idInput = document.getElementById("signupEmail");
  const pwInput = document.getElementById("signupPassword");
  const nicknameInput = document.getElementById("signupNickname");

  const signup = async () => {
    const id = idInput?.value?.trim();
    const pw = pwInput?.value?.trim();
    const nickname = nicknameInput?.value?.trim();

    if (!id || !pw || !nickname) {
      alert("이메일, 비밀번호, 닉네임을 입력해 주세요.");
      return;
    }

    try {
      const res = await fetch("/users/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ id, pw, nickname }),
      });

      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new Error(data.message || "회원가입에 실패했습니다.");
      }

      alert(data.message || "회원가입이 완료되었습니다.");
      window.location.href = "/login";
    } catch (err) {
      alert(`회원가입 실패: ${err.message}`);
    }
  };

  const handleEnter = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      signup();
    }
  };

  signupBtn?.addEventListener("click", signup);
  idInput?.addEventListener("keydown", handleEnter);
  pwInput?.addEventListener("keydown", handleEnter);
  nicknameInput?.addEventListener("keydown", handleEnter);
})();
