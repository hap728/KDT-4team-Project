(() => {
  const recommendedJobsEl = document.getElementById("recommendedJobs");
  if (!recommendedJobsEl) return;

  const CATEGORY_IDS = [1, 2, 3, 4, 5];
  const PAGE_SIZE = 200;
  const LIMIT = 5;

  let allJobs = null;

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

  const getMyStacks = () =>
    uniqueStacks(
      Array.from(document.querySelectorAll("#myStacks .stack-chip")).map((node) =>
        node.textContent.trim()
      )
    );

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return String(dateString).split(" ")[0];
  };

  const formatEmploymentType = (text) => {
    if (!text) return "-";
    return String(text).replace(/\n/g, " / ");
  };

  const setEmpty = (message) => {
    recommendedJobsEl.innerHTML = `<p class="muted">${message}</p>`;
  };

  const fetchCategoryPage = async (categoryId, page) => {
    const response = await fetch(`/api/jobs?categoryId=${categoryId}&page=${page}&size=${PAGE_SIZE}`);
    if (!response.ok) {
      throw new Error("추천 공고 조회에 실패했습니다.");
    }
    return response.json();
  };

  const fetchAllJobs = async () => {
    const jobs = [];

    for (const categoryId of CATEGORY_IDS) {
      const firstPage = await fetchCategoryPage(categoryId, 1);
      jobs.push(...(firstPage.jobs || []));

      const totalPages = Math.max(1, Number(firstPage.totalPages || 1));
      if (totalPages > 1) {
        const pagePromises = [];
        for (let page = 2; page <= totalPages; page += 1) {
          pagePromises.push(fetchCategoryPage(categoryId, page));
        }
        const pageResults = await Promise.all(pagePromises);
        pageResults.forEach((pageData) => {
          jobs.push(...(pageData.jobs || []));
        });
      }
    }

    const deduped = [];
    const seenPostingIds = new Set();

    jobs.forEach((job) => {
      const key = String(job.postingId || `${job.companyName}-${job.jobPosition}-${job.postedDate}`);
      if (seenPostingIds.has(key)) return;
      seenPostingIds.add(key);
      deduped.push(job);
    });

    return deduped;
  };

  const buildRecommendation = (job, myStacks) => {
    const jobStacks = uniqueStacks(String(job.techStack || "").split(","));
    if (!jobStacks.length) return null;

    const myStackKeys = new Set(myStacks.map(normalize));
    const matchedStacks = jobStacks.filter((stack) => myStackKeys.has(normalize(stack)));
    const missingStacks = jobStacks.filter((stack) => !myStackKeys.has(normalize(stack)));
    const score = Math.round((matchedStacks.length / jobStacks.length) * 100);

    return {
      job,
      score,
      jobStacks,
      matchedStacks,
      missingStacks,
    };
  };

  const renderRecommendations = (recommendations) => {
    recommendedJobsEl.innerHTML = "";

    recommendations.forEach((item, index) => {
      const { job, score, jobStacks, matchedStacks, missingStacks } = item;
      const card = document.createElement("article");
      card.className = "recommended-job-card";

      const matchedKeySet = new Set(matchedStacks.map(normalize));
      const stackChips = jobStacks.length
        ? jobStacks
            .map((stack) => {
              const matched = matchedKeySet.has(normalize(stack));
              return `<span class="stack-chip ${matched ? "matched-stack-chip" : ""}">${stack}</span>`;
            })
            .join("")
        : '<span class="muted">기술 스택 정보가 없습니다.</span>';

      const missingText = missingStacks.length ? missingStacks.join(", ") : "없음";
      const actionHtml = job.postingUrl
        ? `<a class="auth-btn secondary" href="${job.postingUrl}" target="_blank" rel="noopener">공고 바로가기</a>`
        : "";

      card.innerHTML = `
        <div class="recommended-job-top">
          <div class="row" style="align-items:flex-start; gap:10px; flex:1;">
            <span class="recommended-rank">${index + 1}</span>
            <div class="recommended-job-head">
              <div class="job-company">${job.companyName ?? "-"}</div>
              <div class="job-title">${job.jobPosition ?? "-"}</div>
              <div class="job-stack-list">${stackChips}</div>
            </div>
          </div>
          <div class="recommended-score">
            <strong>${score}%</strong>
            <span>일치율</span>
          </div>
        </div>
        <div class="recommended-meta">
          <div><strong>근무지역:</strong> ${job.region ?? "-"}</div>
          <div><strong>고용형태:</strong> ${formatEmploymentType(job.employmentType)}</div>
          <div><strong>등록일:</strong> ${formatDate(job.postedDate)}</div>
          <div><strong>마감일:</strong> ${formatDate(job.deadline)}</div>
          <div><strong>부족 스택:</strong> ${missingText}</div>
        </div>
        <div class="recommended-actions">${actionHtml}</div>
      `;

      recommendedJobsEl.appendChild(card);
    });
  };

  const updateRecommendations = async () => {
    const myStacks = getMyStacks();
    if (!myStacks.length) {
      setEmpty("보유 스택을 입력하면 추천 공고를 보여드립니다.");
      return;
    }

    try {
      if (!allJobs) {
        setEmpty("추천 공고를 불러오는 중입니다...");
        allJobs = await fetchAllJobs();
      }

      const recommendations = allJobs
        .map((job) => buildRecommendation(job, myStacks))
        .filter(Boolean)
        .filter((item) => item.score > 0)
        .sort((a, b) => {
          if (b.score !== a.score) return b.score - a.score;
          if (b.matchedStacks.length !== a.matchedStacks.length) {
            return b.matchedStacks.length - a.matchedStacks.length;
          }
          return a.missingStacks.length - b.missingStacks.length;
        })
        .slice(0, LIMIT);

      if (!recommendations.length) {
        setEmpty("현재 보유 스택과 일치하는 추천 공고가 없습니다.");
        return;
      }

      renderRecommendations(recommendations);
    } catch {
      setEmpty("추천 공고를 불러오지 못했습니다.");
    }
  };

  document.addEventListener("matching:stacks-changed", updateRecommendations);
  updateRecommendations();
})();
