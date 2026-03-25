document.addEventListener("DOMContentLoaded", () => {
    const bookmarkList = document.getElementById("bookmarkList");

    const formatDate = (dateString) => {
        if (!dateString) return "-";
        return String(dateString).split(" ")[0];
    };

    const formatEmploymentType = (text) => {
        if (!text) return "-";
        return String(text).replace(/\n/g, " / ");
    };

    const fetchBookmarkedJobs = async () => {
        try {
            const response = await fetch("/api/bookmarks", {
                method: "GET",
                credentials: "include"
            });

            if (response.status === 401) {
                alert("로그인 후 이용 가능합니다.");
                window.location.href = "/login";
                return [];
            }

            if (!response.ok) {
                throw new Error("즐겨찾기 목록 조회 실패");
            }

            return await response.json();
        } catch (error) {
            console.error(error);
            bookmarkList.innerHTML = '<div class="card" style="padding:16px;">즐겨찾기 공고를 불러오지 못했습니다.</div>';
            return [];
        }
    };

    const bindBookmarkEvents = () => {
        const buttons = document.querySelectorAll(".bookmark-btn");

        buttons.forEach((button) => {
            button.addEventListener("click", async (e) => {
                e.preventDefault();

                const postingId = Number(button.dataset.postingId);

                try {
                    const response = await fetch(`/api/bookmarks/${postingId}`, {
                        method: "DELETE",
                        credentials: "include"
                    });

                    if (response.status === 401) {
                        alert("로그인 후 이용 가능합니다.");
                        return;
                    }

                    if (!response.ok) {
                        throw new Error("즐겨찾기 해제 실패");
                    }

                    const card = button.closest(".job-card");
                    if (card) {
                        card.remove();
                    }

                    if (!bookmarkList.querySelector(".job-card")) {
                        bookmarkList.innerHTML = '<div class="card" style="padding:16px;">즐겨찾기한 공고가 없습니다.</div>';
                    }
                } catch (error) {
                    console.error(error);
                }
            });
        });
    };

    const renderBookmarkedJobs = (jobs) => {
        bookmarkList.innerHTML = "";

        if (!jobs || jobs.length === 0) {
            bookmarkList.innerHTML = '<div class="card" style="padding:16px;">즐겨찾기한 공고가 없습니다.</div>';
            return;
        }

        jobs.forEach((job) => {
            const techStacks = job.techStack
                ? job.techStack
                    .split(",")
                    .map((stack) => `<span class="stack-chip">${stack.trim()}</span>`)
                    .join("")
                : "";

            const card = document.createElement("div");
            card.className = "job-card";

            const rawUrl = job.postingUrl ?? "";
            const safeUrl = rawUrl
                ? (rawUrl.startsWith("http://") || rawUrl.startsWith("https://") ? rawUrl : `http://${rawUrl}`)
                : "#";
            const deadlineStatus = getDeadlineStatus(job.deadline);

            card.innerHTML = `
        <div class="job-card-inner">
          <div class="row" style="justify-content:space-between; align-items:flex-start;">
            <div class="job-company">${job.companyName ?? "-"}</div>
            <button
              class="bookmark-btn"
              data-posting-id="${job.postingId}"
              type="button"
              style="border:none; background:none; cursor:pointer; font-size:24px; color:#c9ad4f; padding:0; line-height:1;"
              title="즐겨찾기 해제"
            >
              ★
            </button>
          </div>

          <div class="job-main-row">
            <div class="job-title-area">
              <div class="job-title">${job.jobPosition ?? "-"}</div>
              <div class="job-stack-list">${techStacks}</div>
              <div class="job-link-row">
                <a href="${safeUrl}" target="_blank" rel="noopener" class="job-link-btn">공고 바로가기</a>
              </div>
            </div>

            <div class="job-info-area">
              <div><strong>연봉:</strong> ${job.salary ?? "-"}</div>
              <div><strong>학력:</strong> ${job.education ?? "-"}</div>
              <div><strong>근무지역:</strong> ${job.region ?? "-"}</div>
              <div><strong>고용형태:</strong> ${formatEmploymentType(job.employmentType)}</div>
              <div><strong>등록일:</strong> ${formatDate(job.postedDate)}</div>
              <div><strong>마감일:</strong> ${formatDate(job.deadline)}</div>
              <div><strong>상태:</strong> <span class="${deadlineStatus === "마감" ? "danger" : ""}">${deadlineStatus}</span></div>
            </div>
          </div>
        </div>
      `;

            bookmarkList.appendChild(card);
        });

        bindBookmarkEvents();
    };

    const loadBookmarkedJobs = async () => {
        const jobs = await fetchBookmarkedJobs();
        renderBookmarkedJobs(jobs);
    };

    loadBookmarkedJobs();
});

const getDeadlineStatus = (deadline) => {
    if (!deadline) return "";

    const today = new Date();
    const endDate = new Date(deadline);

    today.setHours(0, 0, 0, 0);
    endDate.setHours(0, 0, 0, 0);

    return endDate < today ? "마감" : "모집중";
};