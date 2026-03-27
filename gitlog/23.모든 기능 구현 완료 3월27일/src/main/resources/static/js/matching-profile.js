
(() => {
  const myStacksEl = document.getElementById("myStacks");
  const stackInput = document.getElementById("stackInput");
  const addStackBtn = document.getElementById("addStack");
  const suggestionBox = document.getElementById("stackSuggestions");
  const storageKey = "matchingMyStacks";

  const myStacks = [];
  let stackCatalog = [];

  const emitStacksChanged = () => {
    document.dispatchEvent(
      new CustomEvent("matching:stacks-changed", {
        detail: { stacks: [...myStacks] },
      })
    );
  };

  const persistStacks = () => {
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(myStacks));
    } catch {
      // ignore
    }
  };

  const loadSavedStacks = () => {
    try {
      const raw = sessionStorage.getItem(storageKey);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  };

  const renderStacks = () => {
    if (!myStacksEl) return;
    myStacksEl.innerHTML = "";

    if (myStacks.length === 0) {
      const empty = document.createElement("span");
      empty.className = "muted";
      empty.textContent = "등록된 스택이 없습니다.";
      myStacksEl.appendChild(empty);
      emitStacksChanged();
      return;
    }

    myStacks.forEach((name, idx) => {
      const chip = document.createElement("span");
      chip.className = "stack-chip";
      chip.textContent = name;
      chip.style.cursor = "pointer";
      chip.title = "클릭해서 제거";
      chip.addEventListener("click", () => {
        myStacks.splice(idx, 1);
        persistStacks();
        renderStacks();
        renderSuggestions(stackInput?.value || "");
      });
      myStacksEl.appendChild(chip);
    });

    emitStacksChanged();
  };

  const hideSuggestions = () => {
    if (!suggestionBox) return;
    suggestionBox.hidden = true;
    suggestionBox.innerHTML = "";
  };

  const fillInput = (rawName) => {
    const name = String(rawName || "").trim();
    if (!name || !stackInput) return;
    stackInput.value = name;
    hideSuggestions();
    stackInput.focus();
  };

  const addStack = (rawName) => {
    const name = String(rawName || "").trim();
    if (!name) return;
    if (!myStacks.includes(name)) {
      myStacks.push(name);
      persistStacks();
      renderStacks();
    }
    if (stackInput) {
      stackInput.value = "";
      stackInput.focus();
    }
    hideSuggestions();
  };

  const renderSuggestions = (keyword) => {
    if (!suggestionBox) return;

    const normalized = String(keyword || "").trim().toLowerCase();
    if (!normalized) {
      hideSuggestions();
      return;
    }

    const matched = stackCatalog
      .filter((name) => name.toLowerCase().includes(normalized) && !myStacks.includes(name))
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
  };

  const mergeStacks = (stacks) => {
    stacks.forEach((name) => {
      const stackName = String(name || "").trim();
      if (stackName && !myStacks.includes(stackName)) {
        myStacks.push(stackName);
      }
    });
  };

  const replaceStacks = (stacks) => {
    myStacks.length = 0;
    mergeStacks(stacks);
  };

  const loadProfileStacks = async () => {
    const savedStacks = loadSavedStacks();

    try {
      const res = await fetch("/users/profile", { credentials: "same-origin" });
      if (res.ok) {
        const data = await res.json();
        if (data.loggedIn) {
          replaceStacks(data.stackNames || []);
          persistStacks();
          renderStacks();

          if (myStacks.length === 0) {
            alert("프로필에서 보유 스택을 추가해주세요.");
            window.location.href = "/profile";
            return;
          }

          return;
        }
      }
    } catch {
      // ignore
    }

    replaceStacks(savedStacks);
    persistStacks();
    renderStacks();
  };

  const loadStackCatalog = async () => {
    try {
      const res = await fetch("/api/stacks", { credentials: "same-origin" });
      if (!res.ok) return;
      const data = await res.json();
      stackCatalog = [...new Set((data || []).map((item) => item.stackName).filter(Boolean))];
    } catch {
      stackCatalog = [];
    }
  };

  addStackBtn?.addEventListener("click", () => addStack(stackInput?.value));

  stackInput?.addEventListener("input", (e) => {
    renderSuggestions(e.target.value);
  });

  stackInput?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addStack(stackInput?.value);
    }
    if (e.key === "Escape") {
      hideSuggestions();
    }
  });

  document.addEventListener("click", (e) => {
    if (!suggestionBox || !stackInput) return;
    if (e.target === stackInput || suggestionBox.contains(e.target)) return;
    hideSuggestions();
  });

  Promise.all([loadProfileStacks(), loadStackCatalog()]).then(() => {
    renderStacks();
  });
})();
