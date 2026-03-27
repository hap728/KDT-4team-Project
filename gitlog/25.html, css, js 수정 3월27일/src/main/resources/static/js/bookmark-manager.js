window.BookmarkManager = (() => {
  let bookmarkedIds = new Set();
  let loaded = false;

  const readMessage = async (response, fallback) => {
    try {
      const data = await response.json();
      return data?.message || fallback;
    } catch {
      return fallback;
    }
  };

  const fetchBookmarkedIds = async () => {
    try {
      const response = await fetch("/api/bookmarks/ids", {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        bookmarkedIds = new Set();
        loaded = true;
        return [];
      }

      const ids = await response.json();
      bookmarkedIds = new Set((ids || []).map((id) => Number(id)));
      loaded = true;
      return [...bookmarkedIds];
    } catch (error) {
      console.error("즐겨찾기 ID 조회 실패:", error);
      bookmarkedIds = new Set();
      loaded = true;
      return [];
    }
  };

  const ensureLoaded = async () => {
    if (!loaded) {
      await fetchBookmarkedIds();
    }
  };

  const isBookmarked = (postingId) => bookmarkedIds.has(Number(postingId));

  const getButtonMarkup = (postingId) => {
    const active = isBookmarked(postingId);
    return `
      <button
        class="bookmark-btn ${active ? "active" : ""}"
        data-posting-id="${postingId}"
        title="즐겨찾기"
        type="button"
      >
        ${active ? "★" : "☆"}
      </button>
    `;
  };

  const syncButton = (button, active) => {
    button.classList.toggle("active", active);
    button.textContent = active ? "★" : "☆";
  };

  const toggleBookmark = async (postingId) => {
    const numericId = Number(postingId);
    const active = isBookmarked(numericId);

    const response = await fetch(`/api/bookmarks/${numericId}`, {
      method: active ? "DELETE" : "POST",
      credentials: "include",
    });

    if (response.status === 401) {
      throw new Error("로그인 후 이용 가능합니다.");
    }

    if (!response.ok) {
      throw new Error(await readMessage(response, "즐겨찾기 처리에 실패했습니다."));
    }

    if (active) {
      bookmarkedIds.delete(numericId);
      return false;
    }

    bookmarkedIds.add(numericId);
    return true;
  };

  const bindBookmarkButtons = (root = document, options = {}) => {
    const { onToggle } = options;
    const buttons = root.querySelectorAll(".bookmark-btn");

    buttons.forEach((button) => {
      if (button.dataset.bookmarkBound === "Y") {
        syncButton(button, isBookmarked(button.dataset.postingId));
        return;
      }

      syncButton(button, isBookmarked(button.dataset.postingId));
      button.dataset.bookmarkBound = "Y";

      button.addEventListener("click", async (event) => {
        event.preventDefault();
        event.stopPropagation();

        try {
          const active = await toggleBookmark(button.dataset.postingId);
          syncButton(button, active);

          if (typeof onToggle === "function") {
            onToggle({
              postingId: Number(button.dataset.postingId),
              active,
              button,
            });
          }
        } catch (error) {
          alert(error.message);
        }
      });
    });
  };

  return {
    ensureLoaded,
    fetchBookmarkedIds,
    isBookmarked,
    getButtonMarkup,
    bindBookmarkButtons,
  };
})();
