const API_BASE = '/api';
let currentToken = localStorage.getItem('token') || null;
let currentUser = null;
let isLogin = true;
let charts = {};
let projectsCache = {};
let allSkillsCache = [];

// DOM Elements
const authModal = document.getElementById('authModal');
const addSkillModal = document.getElementById('addSkillModal');
const createSkillModal = document.getElementById('createSkillModal');
const projectModal = document.getElementById('projectModal');
const findTalentModal = document.getElementById('findTalentModal');
const appContainer = document.getElementById('appContainer');
const landingContainer = document.getElementById('landingContainer');

document.addEventListener('DOMContentLoaded', () => {
    renderHeaderDate();
    setupEventListeners();
    if (currentToken) {
        fetchUserProfile();
    }
});

// ==========================================
// Bootstrapping / event wiring
// ==========================================
function setupEventListeners() {
    // Sidebar navigation
    document.querySelectorAll('.nav-item').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(e.currentTarget.getAttribute('data-target'));
        });
    });

    // Auth toggle (login <-> register)
    document.getElementById('authToggleBtn').addEventListener('click', () => {
        isLogin = !isLogin;
        setAuthMode();
    });

    // Auth submit
    document.getElementById('authForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const fullName = document.getElementById('fullName').value;
        const endpoint = isLogin ? '/auth/login' : '/auth/register';
        const body = isLogin ? { email, password } : { email, password, fullName };
        const errorEl = document.getElementById('authError');
        errorEl.classList.remove('visible');

        try {
            const res = await apiCall(endpoint, 'POST', body);
            currentToken = res.token;
            localStorage.setItem('token', currentToken);
            authModal.close();
            document.getElementById('authForm').reset();
            fetchUserProfile();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    // Logout
    document.getElementById('logoutBtn').addEventListener('click', () => {
        currentToken = null;
        currentUser = null;
        localStorage.removeItem('token');
        Object.keys(charts).forEach(k => { try { charts[k].destroy(); } catch (e) {} });
        charts = {};
        appContainer.style.display = 'none';
        landingContainer.style.display = 'flex';
        document.getElementById('authForm').reset();
    });

    // My Profile: Add Skill
    document.getElementById('openAddSkillBtn').addEventListener('click', async () => {
        await populateSkillDropdown();
        addSkillModal.showModal();
    });

    document.getElementById('addSkillForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const skillId = Number(document.getElementById('skillSelect').value);
        const proficiencyLevel = document.getElementById('proficiencySelect').value;
        const yearsOfExperience = Number(document.getElementById('experienceInput').value);
        const errorEl = document.getElementById('addSkillError');
        errorEl.classList.remove('visible');

        try {
            await apiCall('/users/me/skills', 'POST', { skillId, proficiencyLevel, yearsOfExperience });
            addSkillModal.close();
            document.getElementById('addSkillForm').reset();
            loadMySkills();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    // Skill Catalog: Create Skill
    document.getElementById('openCreateSkillBtn').addEventListener('click', () => {
        document.getElementById('createSkillError').classList.remove('visible');
        createSkillModal.showModal();
    });

    document.getElementById('createSkillForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('newSkillName').value;
        const category = document.getElementById('newSkillCategory').value;
        const errorEl = document.getElementById('createSkillError');
        errorEl.classList.remove('visible');

        try {
            await apiCall('/skills', 'POST', { name, category });
            createSkillModal.close();
            document.getElementById('createSkillForm').reset();
            loadCatalog();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });

    // Talent Search
    document.getElementById('talentSearchBtn').addEventListener('click', async () => {
        const select = document.getElementById('talentSkillSelect');
        const skillId = select.value;
        if (!skillId) return;
        const skillLabel = select.options[select.selectedIndex].text;
        try {
            const results = await apiCall(`/users/search/by-skill?skillId=${skillId}`, 'GET');
            renderTalentSearch(results, skillLabel);
        } catch (err) {
            console.error(err);
        }
    });

    // Projects: Create
    document.getElementById('openCreateProjectBtn').addEventListener('click', async () => {
        await refreshSkillsCache();
        resetProjectForm();
        document.getElementById('projectModalTitle').textContent = 'Create Project';
        document.getElementById('projectFormSubmit').textContent = 'Create Project';
        addReqSkillRow();
        projectModal.showModal();
    });

    document.getElementById('addReqSkillBtn').addEventListener('click', () => addReqSkillRow());

    document.getElementById('projectForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('projectFormId').value;
        const title = document.getElementById('projectTitle').value;
        const description = document.getElementById('projectDescription').value;
        const status = document.getElementById('projectStatus').value;
        const startDate = document.getElementById('projectStartDate').value || null;
        const endDate = document.getElementById('projectEndDate').value || null;

        const requiredSkills = Array.from(document.querySelectorAll('#reqSkillList .req-skill-row'))
            .map(row => ({
                skillId: Number(row.querySelector('.req-skill-select').value),
                requiredProficiency: row.querySelector('.req-prof-select').value
            }))
            .filter(rs => rs.skillId);

        const body = { title, description, status, startDate, endDate, requiredSkills };
        const errorEl = document.getElementById('projectFormError');
        errorEl.classList.remove('visible');

        try {
            if (id) {
                await apiCall('/projects/' + id, 'PUT', body);
            } else {
                await apiCall('/projects', 'POST', body);
            }
            projectModal.close();
            loadProjects();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.add('visible');
        }
    });
}

// ==========================================
// Auth
// ==========================================
function openLoginModal() {
    isLogin = true;
    setAuthMode();
    authModal.showModal();
}

function openRegisterModal() {
    isLogin = false;
    setAuthMode();
    authModal.showModal();
}

function prefillLogin(email) {
    isLogin = true;
    setAuthMode();
    document.getElementById('email').value = email;
    document.getElementById('password').value = 'password123';
    authModal.showModal();
}

function setAuthMode() {
    document.getElementById('nameGroup').style.display = isLogin ? 'none' : 'block';
    document.getElementById('fullName').required = !isLogin;
    document.getElementById('authTitle').textContent = isLogin ? 'Welcome to TeamForge' : 'Create an Account';
    document.getElementById('authSubtitle').textContent = isLogin
        ? 'Log in to manage your talent profile'
        : 'Join as an employee and start building your skill profile';
    document.getElementById('authSubmit').textContent = isLogin ? 'Log In' : 'Register';
    document.getElementById('authToggleText').textContent = isLogin ? "Don't have an account?" : 'Already have an account?';
    document.getElementById('authToggleBtn').textContent = isLogin ? 'Register' : 'Log In';
    document.getElementById('authError').classList.remove('visible');
}

async function fetchUserProfile() {
    try {
        currentUser = await apiCall('/users/me', 'GET');
        document.getElementById('displayUserName').textContent = currentUser.fullName;
        document.getElementById('displayUserRole').textContent = currentUser.role;
        document.getElementById('welcomeHeading').textContent = `Welcome back, ${firstName(currentUser.fullName)}`;
        landingContainer.style.display = 'none';
        appContainer.style.display = 'flex';
        applyRBAC();
        navigateTo('dashboard');
    } catch (err) {
        console.error(err);
        currentToken = null;
        localStorage.removeItem('token');
    }
}

function firstName(fullName) {
    return (fullName || '').split(' ')[0] || 'there';
}

function applyRBAC() {
    if (!currentUser) return;
    const role = currentUser.role;
    document.querySelectorAll('.rbac-manager').forEach(el => {
        if (role === 'EMPLOYEE') el.classList.add('rbac-hidden');
        else el.classList.remove('rbac-hidden');
    });
}

// ==========================================
// Generic API helper
// ==========================================
async function apiCall(endpoint, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (currentToken) {
        headers['Authorization'] = `Bearer ${currentToken}`;
    }

    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);

    const res = await fetch(API_BASE + endpoint, config);
    if (!res.ok) {
        let msg = res.statusText;
        try { const errData = await res.json(); msg = errData.message || JSON.stringify(errData.errors) || msg; } catch (e) {}
        throw new Error(msg);
    }

    const text = await res.text();
    return text ? JSON.parse(text) : {};
}

// ==========================================
// Navigation (single-page sections)
// ==========================================
function navigateTo(target) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    const section = document.getElementById(`section-${target}`);
    if (section) section.classList.add('active');

    document.querySelectorAll('.nav-item').forEach(l => l.classList.remove('active'));
    const navItem = document.querySelector(`.nav-item[data-target="${target}"]`);
    if (navItem) navItem.classList.add('active');

    document.getElementById('pageTitle').textContent = titleFor(target);
    document.getElementById('globalSearchInput').value = '';

    if (target === 'dashboard') loadDashboardSummary();
    else if (target === 'detail-projects') loadProjectDetail();
    else if (target === 'detail-skillset') loadSkillsetDetail();
    else if (target === 'detail-skillgap') loadSkillGapDetail();
    else if (target === 'catalog') loadCatalog();
    else if (target === 'talent') loadTalentSkillOptions();
    else if (target === 'projects') loadProjects();
    else if (target === 'profile') loadProfile();
}

function titleFor(target) {
    const map = {
        'dashboard': 'Dashboard',
        'catalog': 'Skill Catalog',
        'talent': 'Talent Search',
        'projects': 'Projects',
        'profile': 'My Profile',
        'detail-projects': 'Active Projects',
        'detail-skillset': 'Skillset Overview',
        'detail-skillgap': 'Skill Gaps'
    };
    return map[target] || 'TeamForge';
}

function renderHeaderDate() {
    const opts = { weekday: 'long', month: 'long', day: 'numeric' };
    const headerDate = document.getElementById('headerDate');
    if (headerDate) headerDate.textContent = new Date().toLocaleDateString(undefined, opts);
    const welcomeDate = document.getElementById('welcomeDate');
    if (welcomeDate) welcomeDate.textContent = new Date().toLocaleDateString(undefined, { ...opts, year: 'numeric' });
}

window.handleQuickSearch = function (query) {
    const q = query.toLowerCase();
    const activeSection = document.querySelector('.page-section.active');
    if (!activeSection) return;

    const cards = activeSection.querySelectorAll('.card');
    cards.forEach(card => {
        if (card.classList.contains('stat-card') || card.classList.contains('chart-card') ||
            card.classList.contains('data-table-wrap') || card.classList.contains('welcome-banner') ||
            card.id === 'profileInfoCard') return;
        card.style.display = card.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
};

// ==========================================
// Dashboard (metrics only) + detail pages
// ==========================================
async function loadDashboardSummary() {
    try {
        const stats = await apiCall('/dashboard/stats', 'GET');
        document.getElementById('statActiveProjects').textContent = stats.activeProjects;
        document.getElementById('statTotalSkills').textContent = stats.skillsetCount;
        document.getElementById('statSkillGap').textContent = stats.skillGapCount;
    } catch (err) {
        console.error('Failed to load dashboard stats', err);
    }
}

function destroyChart(key) {
    if (charts[key]) {
        try { charts[key].destroy(); } catch (e) {}
        delete charts[key];
    }
}

function chartAxisOptions() {
    return {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { labels: { color: '#cbd5e1' } } },
        scales: {
            x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.06)' } },
            y: { ticks: { color: '#94a3b8' }, beginAtZero: true, grid: { color: 'rgba(255,255,255,0.06)' } }
        }
    };
}

async function loadProjectDetail() {
    try {
        const stats = await apiCall('/dashboard/stats', 'GET');
        const list = stats.projectProgress || [];

        const labels = list.map(p => p.title);
        const data = list.map(p => p.percentComplete == null ? 0 : p.percentComplete);
        const colors = list.map(p => {
            if (p.status === 'COMPLETED') return 'rgba(16,185,129,0.65)';
            if (p.daysRemaining != null && p.daysRemaining < 0) return 'rgba(239,68,68,0.65)';
            return 'rgba(59,130,246,0.65)';
        });

        destroyChart('projectProgress');
        const opts = chartAxisOptions();
        opts.indexAxis = 'y';
        opts.scales.x.max = 100;
        opts.scales.y.beginAtZero = false;
        opts.scales.y.grid.display = false;
        opts.plugins.legend.display = false;

        charts.projectProgress = new Chart(document.getElementById('projectProgressChart'), {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Progress (%)', data, backgroundColor: colors, borderRadius: 6 }] },
            options: opts
        });

        const tbody = document.getElementById('projectDetailTableBody');
        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="color:var(--text-muted);">No projects yet.</td></tr>`;
            return;
        }
        tbody.innerHTML = list.map(p => {
            const pct = p.percentComplete == null ? 0 : p.percentComplete;
            const overdue = p.status !== 'COMPLETED' && p.daysRemaining != null && p.daysRemaining < 0;
            const fillClass = p.status === 'COMPLETED' ? 'done' : (overdue ? 'overdue' : '');
            let daysLeftText;
            if (p.status === 'COMPLETED') daysLeftText = 'Completed';
            else if (p.daysRemaining == null) daysLeftText = 'TBD';
            else if (p.daysRemaining < 0) daysLeftText = `${Math.abs(p.daysRemaining)}d overdue`;
            else daysLeftText = `${p.daysRemaining}d`;

            return `
                <tr>
                    <td>${p.title}</td>
                    <td><span class="pill pill-${p.status.toLowerCase()}">${p.status}</span></td>
                    <td>${p.startDate || 'TBD'}</td>
                    <td>${p.endDate || 'TBD'}</td>
                    <td>${daysLeftText}</td>
                    <td>
                        <div class="progress-row">
                            <div class="progress-track"><div class="progress-fill ${fillClass}" style="width:${pct}%;"></div></div>
                            <span class="progress-pct">${pct}%</span>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        console.error('Failed to load project detail', err);
    }
}

async function loadSkillsetDetail() {
    try {
        const [stats, skills] = await Promise.all([
            apiCall('/dashboard/stats', 'GET'),
            apiCall('/skills', 'GET')
        ]);

        const labels = skills.map(s => s.name);
        const empData = skills.map(s => stats.skillsetDistribution[s.name] || 0);
        const projData = skills.map(s => stats.skillProjectUsage[s.name] || 0);

        destroyChart('skillsetEmployees');
        charts.skillsetEmployees = new Chart(document.getElementById('skillsetEmployeesChart'), {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Employees with Skill', data: empData, backgroundColor: 'rgba(59,130,246,0.55)', borderColor: '#3b82f6', borderWidth: 1, borderRadius: 6 }] },
            options: chartAxisOptions()
        });

        destroyChart('skillsetProjects');
        charts.skillsetProjects = new Chart(document.getElementById('skillsetProjectsChart'), {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Projects Using Skill', data: projData, backgroundColor: 'rgba(139,92,246,0.55)', borderColor: '#8b5cf6', borderWidth: 1, borderRadius: 6 }] },
            options: chartAxisOptions()
        });

        const tbody = document.getElementById('skillsetDetailTableBody');
        tbody.innerHTML = skills.length === 0
            ? `<tr><td colspan="4" style="color:var(--text-muted);">No skills in the catalog yet.</td></tr>`
            : skills.map(s => `
                <tr>
                    <td>${s.name}</td>
                    <td>${s.category}</td>
                    <td>${stats.skillsetDistribution[s.name] || 0}</td>
                    <td>${stats.skillProjectUsage[s.name] || 0}</td>
                </tr>
            `).join('');
    } catch (err) {
        console.error('Failed to load skillset detail', err);
    }
}

async function loadSkillGapDetail() {
    try {
        const [stats, skills] = await Promise.all([
            apiCall('/dashboard/stats', 'GET'),
            apiCall('/skills', 'GET')
        ]);
        const gapSkills = skills.filter(s => s.isTrending);
        const labels = gapSkills.map(s => s.name);
        const empData = gapSkills.map(s => stats.skillGapDistribution[s.name] || 0);
        const demandData = gapSkills.map(s => stats.skillGapDemand[s.name] || 0);

        destroyChart('skillGap');
        charts.skillGap = new Chart(document.getElementById('skillGapChart'), {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    { label: 'Employees With Skill', data: empData, backgroundColor: 'rgba(16,185,129,0.55)', borderColor: '#10b981', borderWidth: 1, borderRadius: 6 },
                    { label: 'Projects Requiring It', data: demandData, backgroundColor: 'rgba(245,158,11,0.55)', borderColor: '#f59e0b', borderWidth: 1, borderRadius: 6 }
                ]
            },
            options: chartAxisOptions()
        });

        const tbody = document.getElementById('skillGapDetailTableBody');
        if (gapSkills.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="color:var(--text-muted);">No trending skill gaps right now.</td></tr>`;
            return;
        }
        tbody.innerHTML = gapSkills.map(s => {
            const emp = stats.skillGapDistribution[s.name] || 0;
            const demand = stats.skillGapDemand[s.name] || 0;
            let statusHtml;
            if (emp === 0) statusHtml = `<span class="pill pill-busy">Critical</span>`;
            else if (emp <= 2) statusHtml = `<span class="pill pill-planning">Watch</span>`;
            else statusHtml = `<span class="pill pill-active">Closing</span>`;
            return `<tr><td>${s.name}</td><td>${s.category}</td><td>${emp}</td><td>${demand}</td><td>${statusHtml}</td></tr>`;
        }).join('');
    } catch (err) {
        console.error('Failed to load skill gap detail', err);
    }
}

// ==========================================
// Skill Catalog
// ==========================================
async function loadCatalog() {
    try {
        const skills = await apiCall('/skills', 'GET');
        const list = document.getElementById('globalCatalogList');
        list.innerHTML = '';

        if (skills.length === 0) {
            list.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">category</span><p>No skills yet.</p></div>`;
            return;
        }

        skills.forEach(s => {
            const item = document.createElement('div');
            item.className = 'catalog-item' + (s.isTrending ? ' gap' : '');
            item.innerHTML = `
                <span class="material-symbols-outlined" style="color:var(--primary-light)">verified</span>
                <span class="cat-name">${s.name}</span>
                <span class="cat-badge">${s.category}</span>
                ${s.isTrending ? '<span class="gap-tag">Market Gap</span>' : ''}
            `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

// ==========================================
// Talent Search
// ==========================================
async function loadTalentSkillOptions() {
    try {
        const skills = await apiCall('/skills', 'GET');
        const select = document.getElementById('talentSkillSelect');
        select.innerHTML = '<option value="">Select a skill...</option>' +
            skills.map(s => `<option value="${s.id}">${s.name} (${s.category})</option>`).join('');
    } catch (err) {
        console.error(err);
    }
}

function renderTalentSearch(results, skillLabel) {
    const grid = document.getElementById('talentGrid');
    grid.innerHTML = '';

    if (results.length === 0) {
        grid.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">person_off</span><p>No employees currently have "${skillLabel}" on their profile.</p></div>`;
        return;
    }

    results.forEach(r => {
        const card = document.createElement('div');
        card.className = 'card';
        const availHtml = r.availableNow
            ? `<span class="pill pill-active">Available Now</span>`
            : `<span class="pill pill-planning">Free on ${r.availableFrom}</span>`;
        card.innerHTML = `
            <div class="card-header">
                <h4>${r.fullName}</h4>
                <span class="badge ${r.proficiencyLevel}">${r.proficiencyLevel}</span>
            </div>
            <div class="card-body" style="flex:1;">
                <p><strong>Email:</strong> ${r.email}</p>
                <p><strong>Dept:</strong> ${r.department || '—'}</p>
                <p><strong>Role:</strong> ${r.designation || '—'}</p>
                <p><strong>Experience:</strong> ${r.yearsOfExperience} ${r.yearsOfExperience === 1 ? 'year' : 'years'}</p>
            </div>
            <div style="margin-top:0.75rem;">${availHtml}</div>
        `;
        grid.appendChild(card);
    });
}

// ==========================================
// Projects
// ==========================================
async function loadProjects() {
    try {
        const projects = await apiCall('/projects', 'GET');
        projectsCache = {};
        projects.forEach(p => { projectsCache[p.id] = p; });
        renderProjectsGrid(projects);
        applyRBAC();
    } catch (err) {
        console.error(err);
    }
}

function renderProjectsGrid(projects) {
    const grid = document.getElementById('projectsGrid');
    grid.innerHTML = '';

    if (projects.length === 0) {
        grid.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">folder_open</span><p>No projects found.</p></div>`;
        return;
    }

    projects.forEach(p => {
        const card = document.createElement('div');
        card.className = 'card';
        const skillChips = (p.requiredSkills || []).length > 0
            ? p.requiredSkills.map(rs => `<span class="skill-chip">${rs.skillName} <span class="chip-level">${rs.requiredProficiency}</span></span>`).join('')
            : `<span class="section-note">No required skills set.</span>`;

        card.innerHTML = `
            <div class="card-header">
                <h4>${p.title}</h4>
                <span class="pill pill-${p.status.toLowerCase()}">${p.status}</span>
            </div>
            <div class="card-body" style="flex:1;">
                <p>${p.description}</p>
                <p style="font-size: 0.8rem; margin-top: 10px; color: var(--text-muted);">
                    ${p.startDate || 'TBD'} to ${p.endDate || 'TBD'}
                </p>
                <div class="chip-row">${skillChips}</div>
                <div id="members-${p.id}" class="section-note" style="display:none; margin-top:0.5rem;"></div>
            </div>
            <div class="card-actions">
                <button class="btn-secondary btn-small rbac-manager" onclick="openFindTalentModal(${p.id})">
                    <span class="material-symbols-outlined" style="font-size:16px;">person_search</span> Find Talent
                </button>
                <button class="btn-secondary btn-small" onclick="toggleTeamView(${p.id})">
                    <span class="material-symbols-outlined" style="font-size:16px;">groups</span> View Team
                </button>
                <button class="btn-secondary btn-small rbac-manager" onclick="openEditProjectModal(${p.id})">
                    <span class="material-symbols-outlined" style="font-size:16px;">edit</span> Edit
                </button>
                <button class="btn-secondary btn-small danger rbac-manager" onclick="deleteProject(${p.id})">
                    <span class="material-symbols-outlined" style="font-size:16px;">delete</span> Delete
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

async function toggleTeamView(projectId) {
    const el = document.getElementById(`members-${projectId}`);
    if (!el) return;
    if (el.style.display === 'block') { el.style.display = 'none'; return; }
    el.style.display = 'block';
    el.innerHTML = 'Loading team…';
    try {
        const members = await apiCall(`/projects/${projectId}/members`, 'GET');
        el.innerHTML = members.length === 0
            ? '<em>No members added yet.</em>'
            : '<strong>Team:</strong> ' + members.map(m => `${m.user.fullName} (${m.role})`).join(', ');
    } catch (err) {
        el.innerHTML = 'Failed to load team.';
    }
}

async function refreshSkillsCache() {
    allSkillsCache = await apiCall('/skills', 'GET');
}

function resetProjectForm() {
    document.getElementById('projectForm').reset();
    document.getElementById('projectFormId').value = '';
    document.getElementById('projectFormError').textContent = '';
    document.getElementById('projectFormError').classList.remove('visible');
    document.getElementById('reqSkillList').innerHTML = '';
}

function addReqSkillRow(selectedSkillId = '', selectedProficiency = 'INTERMEDIATE') {
    const list = document.getElementById('reqSkillList');
    const row = document.createElement('div');
    row.className = 'req-skill-row';

    const skillOptions = allSkillsCache.map(s =>
        `<option value="${s.id}" ${String(s.id) === String(selectedSkillId) ? 'selected' : ''}>${s.name}</option>`
    ).join('');

    row.innerHTML = `
        <select class="req-skill-select">${skillOptions}</select>
        <select class="req-prof-select">
            <option value="BEGINNER" ${selectedProficiency === 'BEGINNER' ? 'selected' : ''}>Beginner</option>
            <option value="INTERMEDIATE" ${selectedProficiency === 'INTERMEDIATE' ? 'selected' : ''}>Intermediate</option>
            <option value="ADVANCED" ${selectedProficiency === 'ADVANCED' ? 'selected' : ''}>Advanced</option>
            <option value="EXPERT" ${selectedProficiency === 'EXPERT' ? 'selected' : ''}>Expert</option>
        </select>
        <button type="button" class="req-skill-remove" title="Remove">
            <span class="material-symbols-outlined" style="font-size:18px;">close</span>
        </button>
    `;
    row.querySelector('.req-skill-remove').addEventListener('click', () => row.remove());
    list.appendChild(row);
}

async function openEditProjectModal(id) {
    await refreshSkillsCache();
    const p = projectsCache[id] || await apiCall(`/projects/${id}`, 'GET');

    resetProjectForm();
    document.getElementById('projectFormId').value = p.id;
    document.getElementById('projectTitle').value = p.title;
    document.getElementById('projectDescription').value = p.description;
    document.getElementById('projectStatus').value = p.status;
    document.getElementById('projectStartDate').value = p.startDate || '';
    document.getElementById('projectEndDate').value = p.endDate || '';
    document.getElementById('projectModalTitle').textContent = 'Edit Project';
    document.getElementById('projectFormSubmit').textContent = 'Save Changes';

    if (p.requiredSkills && p.requiredSkills.length > 0) {
        p.requiredSkills.forEach(rs => addReqSkillRow(rs.skillId, rs.requiredProficiency));
    } else {
        addReqSkillRow();
    }

    projectModal.showModal();
}

async function deleteProject(id) {
    if (!confirm('Delete this project? This also removes its team assignments and skill requirements.')) return;
    try {
        await apiCall(`/projects/${id}`, 'DELETE');
        loadProjects();
    } catch (err) {
        alert('Failed to delete project: ' + err.message);
    }
}

async function openFindTalentModal(id) {
    const p = projectsCache[id];
    document.getElementById('findTalentProjectId').value = id;
    document.getElementById('findTalentTitle').textContent = 'Matching Talent for ' + (p ? p.title : 'Project');
    const resultsEl = document.getElementById('findTalentResults');
    resultsEl.innerHTML = '<p class="section-note">Searching…</p>';
    findTalentModal.showModal();

    try {
        const candidates = await apiCall(`/users/search/for-project/${id}`, 'GET');
        renderFindTalentResults(candidates, id);
    } catch (err) {
        resultsEl.innerHTML = `<p class="section-note">Search failed: ${err.message}</p>`;
    }
}

function renderFindTalentResults(candidates, projectId) {
    const resultsEl = document.getElementById('findTalentResults');
    if (candidates.length === 0) {
        resultsEl.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">person_off</span><p>No employees currently match this project's required skills and availability window.</p></div>`;
        return;
    }
    resultsEl.innerHTML = candidates.map(c => {
        const availHtml = c.availableFrom
            ? `<span class="pill pill-planning">Free on ${c.availableFrom}</span>`
            : `<span class="pill pill-active">Available Now</span>`;
        return `
            <div class="card" style="padding:1rem 1.25rem;" id="candidate-${c.id}">
                <div class="card-header">
                    <div>
                        <strong>${c.fullName}</strong>
                        <div class="card-meta">${c.designation || ''} · ${c.department || ''}</div>
                        <div class="card-meta">${c.email}</div>
                    </div>
                    ${availHtml}
                </div>
                <div class="card-actions">
                    <select id="role-${c.id}" style="background:rgba(0,0,0,0.2); border:1px solid var(--border-glass); color:var(--text-primary); border-radius:8px; padding:0.4rem 0.6rem; font-size:0.8rem;">
                        <option value="CONTRIBUTOR">Contributor</option>
                        <option value="LEAD">Lead</option>
                        <option value="OBSERVER">Observer</option>
                    </select>
                    <button class="btn-primary btn-small" onclick="addCandidateToProject(${projectId}, ${c.id})">
                        <span class="material-symbols-outlined" style="font-size:16px;">person_add</span> Add to Team
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function addCandidateToProject(projectId, userId) {
    const roleSelect = document.getElementById(`role-${userId}`);
    const role = roleSelect ? roleSelect.value : 'CONTRIBUTOR';
    try {
        await apiCall('/projects/members', 'POST', { projectId, userId, role });
        const cardEl = document.getElementById(`candidate-${userId}`);
        if (cardEl) cardEl.innerHTML = '<p class="section-note">✓ Added to the team.</p>';
        loadProjects();
    } catch (err) {
        alert('Failed to add: ' + err.message);
    }
}

// ==========================================
// My Profile
// ==========================================
async function loadProfile() {
    if (!currentUser) return;
    document.getElementById('profileName').textContent = currentUser.fullName;
    document.getElementById('profileMeta').textContent =
        `${currentUser.email} · ${currentUser.department || '—'} · ${currentUser.designation || '—'}`;
    document.getElementById('profileRoleBadge').textContent = currentUser.role;
    loadMySkills();
}

async function populateSkillDropdown() {
    try {
        const skills = await apiCall('/skills', 'GET');
        const select = document.getElementById('skillSelect');
        select.innerHTML = '<option value="" disabled selected>Select a Skill...</option>';
        skills.forEach(s => {
            const opt = document.createElement('option');
            opt.value = s.id;
            opt.textContent = `${s.name} (${s.category})`;
            select.appendChild(opt);
        });
    } catch (err) {
        console.error(err);
    }
}

async function loadMySkills() {
    try {
        const skills = await apiCall('/users/me/skills', 'GET');
        const grid = document.getElementById('mySkillsGrid');
        grid.innerHTML = '';

        if (skills.length === 0) {
            grid.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">stars</span><p>You haven't added any skills yet.</p></div>`;
            return;
        }

        skills.forEach(us => {
            const card = document.createElement('div');
            card.className = 'card';
            card.innerHTML = `
                <div class="card-header">
                    <span class="card-title">${us.skill.name}</span>
                    <span class="badge ${us.proficiencyLevel}">${us.proficiencyLevel}</span>
                </div>
                <div class="card-meta">
                    <span class="material-symbols-outlined" style="font-size:16px;">category</span>
                    ${us.skill.category}
                </div>
                <div class="card-meta" style="margin-top:auto; padding-top:10px;">
                    <span class="material-symbols-outlined" style="font-size:16px;">schedule</span>
                    ${us.yearsOfExperience} ${us.yearsOfExperience === 1 ? 'Year' : 'Years'} Exp
                </div>
                <button class="icon-btn" onclick="removeSkill(${us.id})" style="position:absolute; bottom:15px; right:15px;">
                    <span class="material-symbols-outlined">delete</span>
                </button>
            `;
            grid.appendChild(card);
        });
    } catch (err) {
        console.error('Failed to load skills', err);
    }
}

async function removeSkill(id) {
    if (!confirm('Remove this skill from your profile?')) return;
    try {
        await apiCall(`/users/me/skills/${id}`, 'DELETE');
        loadMySkills();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}
