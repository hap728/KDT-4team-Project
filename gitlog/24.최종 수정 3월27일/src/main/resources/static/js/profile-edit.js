(() => {
  const saveBtn = document.getElementById("profileSaveAllBtn");
  const idInput = document.getElementById("profileId");
  const nickInput = document.getElementById("profileNickname");
  const introInput = document.getElementById("profileIntro");
  const pwInput = document.getElementById("profileNewPw");

  const stackInput = document.getElementById("profileStackInput");
  const stackAddBtn = document.getElementById("profileStackAdd");
  const suggestionBox = document.getElementById("profileStackSuggestions");
  const stackList = document.getElementById("profileStacks");
  const stackStatus = document.getElementById("profileStackStatus");

  const githubConnectBtn = document.getElementById("githubConnectBtn");
  const githubDisconnectBtn = document.getElementById("githubDisconnectBtn");
  const githubLoadBtn = document.getElementById("githubStackLoadBtn");
  const githubApplyBtn = document.getElementById("githubStackApplyBtn");
  const githubApplySection = document.getElementById("githubStackApplySection");
  const githubStatus = document.getElementById("githubStackImportStatus");
  const githubCandidates = document.getElementById("githubStackCandidates");
  const githubConnectionLabel = document.getElementById("githubConnectionLabel");
  const githubConnectionMeta = document.getElementById("githubConnectionMeta");

  const stacks = [];
  let stackCatalog = [];
  let githubRepositories = [];
  let activeSuggestionIndex = -1;
  let githubAgreeYn = "N";
  let githubLinked = false;
  let githubSessionLinked = false;
  let githubConnectedLogin = "";
  let githubConnectUrl = "/github/connect?returnTo=/profile-edit";

  const setStatus = (message) => {
    if (stackStatus) {
      stackStatus.textContent = message;
    }
  };

  const setGithubStatus = (message) => {
    if (githubStatus) {
      githubStatus.textContent = message;
    }
  };

  const uniqueTrimmed = (values) => {
    const set = new Set();
    (values || []).forEach((value) => {
      const trimmed = String(value || "").trim();
      if (trimmed) {
        set.add(trimmed);
      }
    });
    return [...set];
  };

  const renderStacks = () => {
    if (!stackList) return;

    stackList.innerHTML = "";

    if (stacks.length === 0) {
      const empty = document.createElement("span");
      empty.className = "muted";
      empty.textContent = "등록된 스택이 없습니다.";
      stackList.appendChild(empty);
      return;
    }

    stacks.forEach((name, index) => {
      const chip = document.createElement("span");
      chip.className = "stack-chip";
      chip.textContent = name;
      chip.style.cursor = "pointer";
      chip.title = "클릭해서 제거";
      chip.addEventListener("click", () => {
        stacks.splice(index, 1);
        renderStacks();
        renderGithubCandidates();
        setStatus("스택 목록을 수정했습니다.");
      });
      stackList.appendChild(chip);
    });
  };

  const hideSuggestions = () => {
    if (!suggestionBox) return;
    suggestionBox.hidden = true;
    suggestionBox.innerHTML = "";
    activeSuggestionIndex = -1;
  };

  const fillInput = (rawName) => {
    const name = String(rawName || "").trim();
    if (!name || !stackInput) return;
    stackInput.value = name;
    hideSuggestions();
    stackInput.focus();
  };

  const syncActiveSuggestion = () => {
    if (!suggestionBox || suggestionBox.hidden) return;

    const items = Array.from(suggestionBox.querySelectorAll(".stack-suggestion-item"));
    items.forEach((item, index) => {
      item.classList.toggle("active", index === activeSuggestionIndex);
    });

    if (activeSuggestionIndex >= 0 && activeSuggestionIndex < items.length) {
      items[activeSuggestionIndex].scrollIntoView({ block: "nearest" });
    }
  };

  const renderSuggestions = (keyword) => {
    if (!suggestionBox) return;

    const normalized = String(keyword || "").trim().toLowerCase();
    if (!normalized) {
      hideSuggestions();
      return;
    }

    const matched = stackCatalog
      .filter((name) => name.toLowerCase().includes(normalized) && !stacks.includes(name))
      .slice(0, 8);

    if (matched.length === 0) {
      hideSuggestions();
      return;
    }

    suggestionBox.hidden = false;
    suggestionBox.innerHTML = "";

    matched.forEach((name) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "stack-suggestion-item";
      button.textContent = name;
      button.addEventListener("click", () => fillInput(name));
      suggestionBox.appendChild(button);
    });

    activeSuggestionIndex = 0;
    syncActiveSuggestion();
  };

  const addStack = () => {
    const name = stackInput?.value?.trim();
    if (!name) return;

    if (!stacks.includes(name)) {
      stacks.push(name);
      renderStacks();
      renderGithubCandidates();
      setStatus("스택 목록을 수정했습니다.");
    }

    if (stackInput) {
      stackInput.value = "";
      stackInput.focus();
    }

    hideSuggestions();
  };

  const updateGithubConnectionUi = () => {
    if (githubConnectBtn) {
      githubConnectBtn.textContent = githubLinked ? "GitHub 다시 연동" : "GitHub 연동";
    }

    if (githubDisconnectBtn) {
      githubDisconnectBtn.hidden = !githubSessionLinked;
    }

    if (githubLoadBtn) {
      githubLoadBtn.disabled = false;
    }

    if (githubConnectionLabel) {
      githubConnectionLabel.textContent = githubLinked
        ? githubConnectedLogin
          ? `연결된 GitHub 계정: @${githubConnectedLogin}`
          : "GitHub 계정이 연결되었습니다."
        : "GitHub 연동이 필요합니다.";
    }

    if (githubConnectionMeta) {
      if (githubLinked) {
        githubConnectionMeta.textContent = githubSessionLinked
          ? "현재 로그인 계정에 별도로 연동된 GitHub 레포지토리에서 스택 후보를 가져올 수 있습니다."
          : "GitHub 로그인 세션을 사용해 레포지토리 스택 후보를 가져올 수 있습니다.";
      } else {
        githubConnectionMeta.textContent =
          "일반 로그인과 소셜 로그인 모두 GitHub 연동 후 레포지토리 기술 스택을 가져올 수 있습니다.";
      }
    }
  };

  const updateGithubApplyVisibility = () => {
    if (!githubApplySection) return;
    githubApplySection.style.display = githubRepositories.length === 0 ? "none" : "flex";
  };

  const renderGithubCandidates = () => {
    if (!githubCandidates) return;

    githubCandidates.innerHTML = "";
    updateGithubApplyVisibility();

    if (!githubRepositories.length) {
      const empty = document.createElement("p");
      empty.className = "muted";
      empty.textContent = githubLinked
        ? "GitHub 레포지토리를 불러오면 스택 후보가 여기에 표시됩니다."
        : "먼저 GitHub 계정을 연동해 주세요.";
      githubCandidates.appendChild(empty);
      return;
    }

    githubRepositories.forEach((repo, repoIndex) => {
      const card = document.createElement("section");
      card.className = "github-repo-card";

      const header = document.createElement("div");
      header.className = "github-repo-header";

      const titleWrap = document.createElement("div");
      titleWrap.className = "github-repo-title-wrap";

      const titleLink = document.createElement("a");
      titleLink.href = repo.url || "#";
      titleLink.target = "_blank";
      titleLink.rel = "noreferrer";
      titleLink.innerHTML = `<strong>${repo.fullName || repo.name}</strong>`;
      titleWrap.appendChild(titleLink);

      if (repo.description) {
        const desc = document.createElement("p");
        desc.className = "github-repo-description";
        desc.textContent = repo.description;
        titleWrap.appendChild(desc);
      }

      const meta = document.createElement("div");
      meta.className = "github-repo-meta";
      meta.innerHTML = `<span>${repo.isPrivate ? "비공개" : "공개"}</span><span>★ ${repo.stars ?? 0}</span>`;

      header.appendChild(titleWrap);
      header.appendChild(meta);
      card.appendChild(header);

      const stackGrid = document.createElement("div");
      stackGrid.className = "github-repo-stack-grid";

      (repo.stackItems || []).forEach((stack, stackIndex) => {
        const label = document.createElement("label");
        label.className = "github-stack-option";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = stack.name;
        checkbox.dataset.repoIndex = String(repoIndex);
        checkbox.dataset.stackIndex = String(stackIndex);
        checkbox.disabled = stacks.includes(stack.name);
        checkbox.checked = Boolean(stack.checked) && !checkbox.disabled;

        checkbox.addEventListener("change", () => {
          githubRepositories[repoIndex].stackItems[stackIndex].checked = checkbox.checked;
        });

        const body = document.createElement("div");
        body.className = "github-stack-option-body";

        const titleRow = document.createElement("div");
        titleRow.className = "github-stack-option-title";
        titleRow.innerHTML = `<strong>${stack.name}</strong>`;

        const hint = document.createElement("div");
        hint.className = "github-stack-option-meta";
        hint.textContent = checkbox.disabled
          ? "이미 보유 기술에 포함된 스택입니다."
          : "이 레포지토리에서 감지된 기술 스택입니다.";

        body.appendChild(titleRow);
        body.appendChild(hint);

        if (checkbox.disabled) {
          const badge = document.createElement("span");
          badge.className = "github-stack-option-badge";
          badge.textContent = "이미 보유";
          body.appendChild(badge);
        }

        label.appendChild(checkbox);
        label.appendChild(body);
        stackGrid.appendChild(label);
      });

      card.appendChild(stackGrid);
      githubCandidates.appendChild(card);
    });

    updateGithubApplyVisibility();
  };

  const ensureGithubConsent = async () => {
    if (githubAgreeYn === "Y") {
      return true;
    }

    const consented = window.confirm(
      "GitHub 레포지토리 정보를 읽어 기술 스택 후보를 분석할까요?\n선택한 스택만 프로필에 반영됩니다."
    );

    if (!consented) {
      setGithubStatus("GitHub 접근 동의를 취소했습니다.");
      return false;
    }

    const response = await fetch("/users/profile/github-consent", {
      method: "PUT",
      credentials: "same-origin",
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload?.message || "GitHub 접근 동의 저장에 실패했습니다.");
    }

    githubAgreeYn = String(payload.githubAgreeYn || "Y").toUpperCase();
    return true;
  };

  const loadGithubCandidates = async () => {
    if (!githubLinked) {
      setGithubStatus("먼저 GitHub 계정을 연동해 주세요.");
      return;
    }

    try {
      const allowed = await ensureGithubConsent();
      if (!allowed) {
        return;
      }
    } catch (error) {
      setGithubStatus(`GitHub 접근 동의를 저장하지 못했습니다. ${error.message}`);
      return;
    }

    setGithubStatus("GitHub 레포지토리를 분석하는 중입니다...");

    try {
      const response = await fetch("/api/github/stacks", {
        credentials: "same-origin",
      });

      const payload = await response.json();
      if (!response.ok || !payload.available) {
        githubRepositories = [];
        renderGithubCandidates();
        setGithubStatus(payload?.message || "GitHub 후보를 불러오지 못했습니다.");
        return;
      }

      githubRepositories = (payload.repositories || [])
        .map((repo) => ({
          ...repo,
          stackItems: uniqueTrimmed(repo.stacks).map((name) => ({
            name,
            checked: false,
          })),
        }))
        .filter((repo) => repo.stackItems.length > 0);

      renderGithubCandidates();
      setGithubStatus(payload.message || "GitHub 후보를 불러왔습니다.");
    } catch (error) {
      githubRepositories = [];
      renderGithubCandidates();
      setGithubStatus(`GitHub 후보를 불러오지 못했습니다. ${error.message}`);
    }
  };

  const applyGithubCandidates = () => {
    const checkedNames = githubRepositories.flatMap((repo) =>
      (repo.stackItems || [])
        .filter((stack) => stack.checked)
        .map((stack) => stack.name)
    );

    if (!checkedNames.length) {
      alert("반영할 GitHub 스택을 먼저 선택해 주세요.");
      return;
    }

    uniqueTrimmed([...stacks, ...checkedNames]).forEach((name) => {
      if (!stacks.includes(name)) {
        stacks.push(name);
      }
    });

    githubRepositories = githubRepositories.map((repo) => ({
      ...repo,
      stackItems: (repo.stackItems || []).map((stack) => ({
        ...stack,
        checked: false,
      })),
    }));

    renderStacks();
    renderGithubCandidates();
    setStatus("스택 목록을 수정했습니다.");
    setGithubStatus("선택한 GitHub 스택을 보유 기술 스택에 반영했습니다.");
  };

  const disconnectGithub = async () => {
    try {
      const response = await fetch("/github/connect", {
        method: "DELETE",
        credentials: "same-origin",
      });
      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload?.message || "GitHub 연동 해제에 실패했습니다.");
      }

      githubLinked = Boolean(payload.githubLinked);
      githubSessionLinked = false;
      githubConnectedLogin = "";
      githubRepositories = [];

      updateGithubConnectionUi();
      renderGithubCandidates();
      setGithubStatus(payload.message || "GitHub 연동이 해제되었습니다.");
    } catch (error) {
      setGithubStatus(error.message);
    }
  };

  const saveProfile = async () => {
    const id = idInput?.value?.trim();
    const nickname = nickInput?.value?.trim() ?? "";
    const intro = introInput?.value?.trim() ?? "";
    const newPassword = pwInput?.value?.trim() ?? "";

    if (!id) {
      alert("로그인 ID를 확인할 수 없습니다. 다시 로그인해 주세요.");
      return;
    }

    try {
      const response = await fetch("/users/profile", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({
          id,
          nickname,
          intro,
          newPassword,
          stackNames: stacks,
        }),
      });

      let payload = null;
      try {
        payload = await response.json();
      } catch {
        payload = null;
      }

      if (!response.ok) {
        throw new Error(payload?.message || "저장에 실패했습니다.");
      }

      setStatus("저장이 완료되었습니다.");
      alert(payload?.message || "회원 정보가 저장되었습니다.");
      window.location.href = "/profile";
    } catch (error) {
      alert(`저장 실패: ${error.message}`);
    }
  };

  const loadProfile = async () => {
    try {
      const response = await fetch("/users/profile", {
        credentials: "same-origin",
      });

      if (!response.ok) {
        renderStacks();
        updateGithubConnectionUi();
        renderGithubCandidates();
        return;
      }

      const data = await response.json();
      if (!data.loggedIn) {
        renderStacks();
        updateGithubConnectionUi();
        renderGithubCandidates();
        return;
      }

      if (idInput) idInput.value = data.id || "";
      if (nickInput) nickInput.value = data.nickname || "";
      if (introInput) introInput.value = data.intro || "";

      githubAgreeYn = String(data.githubAgreeYn || "N").toUpperCase();
      githubLinked = Boolean(data.githubLinked);
      githubSessionLinked = Boolean(data.githubSessionLinked);
      githubConnectedLogin = String(data.githubConnectedLogin || "").trim();
      githubConnectUrl = String(data.githubConnectUrl || githubConnectUrl);

      stacks.length = 0;
      uniqueTrimmed(data.stackNames).forEach((name) => stacks.push(name));

      renderStacks();
      updateGithubConnectionUi();
      renderGithubCandidates();
      setStatus("수정할 내용을 확인해 주세요.");

      if (data.githubLinkMessage) {
        setGithubStatus(data.githubLinkMessage);
      } else if (githubLinked) {
        setGithubStatus(
          githubConnectedLogin
            ? `GitHub @${githubConnectedLogin} 계정이 연결되어 있습니다.`
            : "GitHub 계정이 연결되어 있습니다."
        );
      } else {
        setGithubStatus("GitHub 연동 후 레포지토리에서 기술 스택을 가져올 수 있습니다.");
      }
    } catch {
      renderStacks();
      updateGithubConnectionUi();
      renderGithubCandidates();
    }
  };

  const loadStackCatalog = async () => {
    try {
      const response = await fetch("/api/stacks", {
        credentials: "same-origin",
      });

      if (!response.ok) {
        stackCatalog = [];
        return;
      }

      const data = await response.json();
      stackCatalog = uniqueTrimmed((data || []).map((item) => item.stackName));
    } catch {
      stackCatalog = [];
    }
  };

  saveBtn?.addEventListener("click", saveProfile);
  stackAddBtn?.addEventListener("click", addStack);
  githubLoadBtn?.addEventListener("click", loadGithubCandidates);
  githubApplyBtn?.addEventListener("click", applyGithubCandidates);
  githubConnectBtn?.addEventListener("click", () => {
    window.location.href = githubConnectUrl;
  });
  githubDisconnectBtn?.addEventListener("click", disconnectGithub);

  stackInput?.addEventListener("input", (event) => {
    renderSuggestions(event.target.value);
  });

  stackInput?.addEventListener("keydown", (event) => {
    const items = suggestionBox
      ? Array.from(suggestionBox.querySelectorAll(".stack-suggestion-item"))
      : [];

    if (event.key === "ArrowDown" && items.length) {
      event.preventDefault();
      activeSuggestionIndex = (activeSuggestionIndex + 1 + items.length) % items.length;
      syncActiveSuggestion();
      return;
    }

    if (event.key === "ArrowUp" && items.length) {
      event.preventDefault();
      activeSuggestionIndex = (activeSuggestionIndex - 1 + items.length) % items.length;
      syncActiveSuggestion();
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();
      if (items.length && activeSuggestionIndex >= 0 && activeSuggestionIndex < items.length) {
        fillInput(items[activeSuggestionIndex].textContent);
        return;
      }
      addStack();
      return;
    }

    if (event.key === "Escape") {
      hideSuggestions();
    }
  });

  document.addEventListener("click", (event) => {
    if (!suggestionBox || !stackInput) return;
    if (event.target === stackInput || suggestionBox.contains(event.target)) return;
    hideSuggestions();
  });

  Promise.all([loadProfile(), loadStackCatalog()]).then(() => {
    renderStacks();
    updateGithubConnectionUi();
    renderGithubCandidates();
  });
})();
