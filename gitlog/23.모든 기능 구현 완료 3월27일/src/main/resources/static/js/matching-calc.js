(() => {
  const calcBtn = document.getElementById("calcMatch");
  const scoreEl = document.getElementById("matchScore");
  const suggestEl = document.getElementById("matchSuggest");
  const haveEl = document.getElementById("haveStacks");
  const missingEl = document.getElementById("missingStacks");
  const boostSelect = document.getElementById("boostStack");
  const simulateBtn = document.getElementById("simulateBtn");
  const simulateResult = document.getElementById("simulateResult");
  const selectedJobDetail = document.getElementById("selectedJobDetail");

  if (!calcBtn || !scoreEl || !suggestEl || !haveEl || !missingEl || !boostSelect || !simulateBtn || !simulateResult || !selectedJobDetail) {
    return;
  }

  let lastResult = null;
  let profileStatePromise = null;

  const normalize = (value) => String(value || "").trim().toLowerCase();

  const uniqueStacks = (values) => {
    const seen = new Set();
    const result = [];
    values.forEach((value) => {
      const raw = String(value || "").trim();
      const key = normalize(raw);
      if (!raw || seen.has(key)) return;
      seen.add(key);
      result.push(raw);
    });
    return result;
  };

  const getMyStacks = () => {
    return Array.from(document.querySelectorAll("#myStacks .stack-chip")).map((node) => node.textContent.trim());
  };

  const getJobStacks = () => {
    const techStack = selectedJobDetail.dataset.techStack || "";
    return uniqueStacks(techStack.split(","));
  };

  const getProfileState = async () => {
    if (!profileStatePromise) {
      profileStatePromise = fetch("/users/profile", { credentials: "same-origin" })
        .then((res) => {
          if (!res.ok) {
            throw new Error("profile fetch failed");
          }
          return res.json();
        })
        .catch(() => ({ loggedIn: false, stackNames: [] }));
    }

    return profileStatePromise;
  };

  const ensureMatchingAccess = async () => {
    const profile = await getProfileState();

    if (!profile?.loggedIn) {
      alert("로그인 후 이용해주세요.");
      window.location.href = "/login";
      return false;
    }

    const profileStacks = uniqueStacks(profile.stackNames || []);
    if (!profileStacks.length) {
      alert("프로필에서 보유 스택을 추가해주세요.");
      window.location.href = "/profile";
      return false;
    }

    return true;
  };

  const renderTagList = (container, stacks, emptyText) => {
    container.innerHTML = "";
    if (!stacks.length) {
      const empty = document.createElement("span");
      empty.className = "muted";
      empty.textContent = emptyText;
      container.appendChild(empty);
      return;
    }
    stacks.forEach((stack) => {
      const chip = document.createElement("span");
      chip.className = "stack-chip";
      chip.textContent = stack;
      container.appendChild(chip);
    });
  };

  const renderBoostOptions = (stacks) => {
    boostSelect.innerHTML = "";
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "부족 스택 선택";
    boostSelect.appendChild(placeholder);

    stacks.forEach((stack) => {
      const option = document.createElement("option");
      option.value = stack;
      option.textContent = stack;
      boostSelect.appendChild(option);
    });
  };

  const getMessageByScore = (score, missingCount) => {
    if (missingCount === 0) return "공고 요구 스택과 모두 일치합니다.";
    if (score >= 80) return "지원 가능한 수준입니다. 부족 스택만 보완해보세요.";
    if (score >= 60) return "핵심 스택은 일부 맞습니다. 부족 스택 보완이 필요합니다.";
    return "현재는 요구 스택 대비 부족한 부분이 많습니다.";
  };

  const calculate = () => {
    const myStacks = uniqueStacks(getMyStacks());
    const jobStacks = getJobStacks();

    if (!jobStacks.length) {
      scoreEl.textContent = "--%";
      suggestEl.textContent = "세부 설정에서 공고를 선택한 뒤 계산해주세요.";
      renderTagList(haveEl, [], "보유 스택이 없습니다.");
      renderTagList(missingEl, [], "부족 스택이 없습니다.");
      renderBoostOptions([]);
      simulateResult.textContent = "부족 스택이 있을 때 시뮬레이션이 가능합니다.";
      lastResult = null;
      return;
    }

    const myStackKeys = new Set(myStacks.map(normalize));
    const haveStacks = jobStacks.filter((stack) => myStackKeys.has(normalize(stack)));
    const missingStacks = jobStacks.filter((stack) => !myStackKeys.has(normalize(stack)));
    const score = Math.round((haveStacks.length / jobStacks.length) * 100);

    scoreEl.textContent = `${score}%`;
    suggestEl.textContent = getMessageByScore(score, missingStacks.length);
    renderTagList(haveEl, haveStacks, "일치하는 스택이 없습니다.");
    renderTagList(missingEl, missingStacks, "부족 스택이 없습니다.");
    renderBoostOptions(missingStacks);
    simulateResult.textContent = "부족 스택이 있을 때 시뮬레이션이 가능합니다.";

    lastResult = {
      score,
      total: jobStacks.length,
      haveCount: haveStacks.length,
      missingStacks,
    };
  };

  calcBtn.addEventListener("click", async () => {
    const canUseMatching = await ensureMatchingAccess();
    if (!canUseMatching) return;
    calculate();
  });

  simulateBtn.addEventListener("click", async () => {
    const canUseMatching = await ensureMatchingAccess();
    if (!canUseMatching) return;

    if (!lastResult) {
      simulateResult.textContent = "먼저 지수 계산을 실행해주세요.";
      return;
    }

    const selectedStack = boostSelect.value;
    if (!selectedStack) {
      simulateResult.textContent = "부족 스택을 선택해주세요.";
      return;
    }

    const nextScore = Math.round(((lastResult.haveCount + 1) / lastResult.total) * 100);
    simulateResult.textContent = `${selectedStack} 스택을 보완하면 예상 매칭 점수는 ${nextScore}%입니다.`;
  });
})();
