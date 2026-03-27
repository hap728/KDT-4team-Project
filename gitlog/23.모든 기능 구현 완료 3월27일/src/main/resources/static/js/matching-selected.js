document.addEventListener("DOMContentLoaded", () => {
  const container = document.getElementById("selectedJobDetail");
  if (!container) return;

  const normalize = (value) => String(value || "").trim().toLowerCase();
  let selectedJob = null;

  const getMyStacks = () =>
    Array.from(document.querySelectorAll("#myStacks .stack-chip"))
      .map((node) => node.textContent.trim())
      .filter(Boolean);

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return String(dateString).split(" ")[0];
  };

  const formatEmploymentType = (text) => {
    if (!text) return "-";
    return String(text).replace(/\n/g, " / ");
  };

  const renderEmpty = () => {
    container.innerHTML = '<div class="muted">선택된 공고가 없습니다.</div>';
    container.dataset.techStack = "";
    container.dataset.jobTitle = "";
  };

  const renderSelectedJob = () => {
    if (!selectedJob) {
      renderEmpty();
      return;
    }

    const myStackKeys = new Set(getMyStacks().map(normalize));
    const techStacks = selectedJob.techStack
      ? selectedJob.techStack
          .split(",")
          .map((stack) => {
            const trimmed = stack.trim();
            const matched = myStackKeys.has(normalize(trimmed));
            return `<span class="stack-chip" style="${
              matched ? "background:#0f8a73;color:#fff;border-color:#0f8a73;" : ""
            }">${trimmed}</span>`;
          })
          .join("")
      : "";

    container.innerHTML = `
      <div class="job-card">
        <div class="job-card-inner">
          <div class="job-company">${selectedJob.companyName ?? "-"}</div>
          <div class="job-main-row">
            <div class="job-title-area">
              <div class="job-title">${selectedJob.jobPosition ?? "-"}</div>
              <div class="job-stack-list">${techStacks}</div>
            </div>
            <div class="job-info-area">
              <div><strong>연봉:</strong> ${selectedJob.salary ?? "-"}</div>
              <div><strong>학력:</strong> ${selectedJob.education ?? "-"}</div>
              <div><strong>근무지역:</strong> ${selectedJob.region ?? "-"}</div>
              <div><strong>고용형태:</strong> ${formatEmploymentType(selectedJob.employmentType)}</div>
              <div><strong>등록일:</strong> ${formatDate(selectedJob.postedDate)}</div>
              <div><strong>마감일:</strong> ${formatDate(selectedJob.deadline)}</div>
            </div>
          </div>
        </div>
      </div>
    `;

    container.dataset.techStack = selectedJob.techStack || "";
    container.dataset.jobTitle = selectedJob.jobPosition || "";
  };

  const raw = localStorage.getItem("selectedMatchingJob");
  if (raw) {
    try {
      selectedJob = JSON.parse(raw);
    } catch {
      selectedJob = null;
    }
  }

  renderSelectedJob();

  if (raw) {
    localStorage.removeItem("selectedMatchingJob");
  }

  document.addEventListener("matching:stacks-changed", () => {
    renderSelectedJob();
  });
});
