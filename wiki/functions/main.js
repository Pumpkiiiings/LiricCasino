document.addEventListener('DOMContentLoaded', () => {
    // --- Estado de la Aplicación ---
    let currentLang = localStorage.getItem('wiki_lang') || 'es';
    let currentTheme = localStorage.getItem('wiki_theme') || 'dark';
    let currentSection = 'introduccion';

    const contentArea = document.getElementById('content-area');
    const langToggleBtn = document.getElementById('langToggle');
    const themeToggleBtn = document.getElementById('themeToggle');
    const themeIconDark = document.getElementById('themeIconDark');
    const themeIconLight = document.getElementById('themeIconLight');

    // --- Inicialización ---
    initTheme();
    updateUI();
    loadSection(currentSection);

    // --- Cursor Trail ---
    const cursorTrail = document.getElementById('cursorTrail');
    document.addEventListener('mousemove', (e) => {
        cursorTrail.style.left = e.clientX + 'px';
        cursorTrail.style.top = e.clientY + 'px';
        
        // Reiniciar animación clonando el elemento
        cursorTrail.classList.remove('trail-fade');
        void cursorTrail.offsetWidth; // trigger reflow
        cursorTrail.classList.add('trail-fade');
    });

    // --- Cambio de Idioma ---
    langToggleBtn.addEventListener('click', () => {
        currentLang = currentLang === 'es' ? 'en' : 'es';
        localStorage.setItem('wiki_lang', currentLang);
        updateUI();
        loadSection(currentSection);
    });

    // --- Cambio de Tema ---
    themeToggleBtn.addEventListener('click', () => {
        currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
        localStorage.setItem('wiki_theme', currentTheme);
        initTheme();
    });

    function initTheme() {
        if (currentTheme === 'light') {
            document.documentElement.setAttribute('data-theme', 'light');
            themeIconDark.style.display = 'block';
            themeIconLight.style.display = 'none';
        } else {
            document.documentElement.removeAttribute('data-theme');
            themeIconDark.style.display = 'none';
            themeIconLight.style.display = 'block';
        }
    }

    // --- Navegación ---
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const section = e.target.getAttribute('data-section');
            if (section) {
                // Update Active state
                navItems.forEach(nav => nav.classList.remove('active'));
                e.target.classList.add('active');
                loadSection(section);
            }
        });
    });

    // File Modal Logic
    const fileModalOverlay = document.getElementById('fileModalOverlay');
    const fileModalTitle = document.getElementById('fileModalTitle');
    const fileModalCode = document.getElementById('fileModalCode');
    const fileModalClose = document.getElementById('fileModalClose');

    function openFileModal(fileName) {
        if (!window.WikiFiles) return;
        const content = window.WikiFiles[fileName] || 'Archivo no encontrado / Empty file';
        fileModalTitle.textContent = fileName;
        fileModalCode.textContent = content;
        fileModalOverlay.classList.add('active');
    }

    function closeFileModal() {
        fileModalOverlay.classList.remove('active');
    }

    if (fileModalClose && fileModalOverlay) {
        fileModalClose.addEventListener('click', closeFileModal);
        fileModalOverlay.addEventListener('click', (e) => {
            if (e.target === fileModalOverlay) closeFileModal();
        });
    }

    // Event delegation for clickable files
    document.addEventListener('click', (e) => {
        const fileEl = e.target.closest('.clickable-file');
        if (fileEl) {
            const fileName = fileEl.getAttribute('data-file');
            if (fileName) {
                openFileModal(fileName);
            }
        }
    });

    // --- Cargar Sección ---
    function loadSection(sectionId) {
        currentSection = sectionId;
        const sectionData = window.WikiSections[sectionId];
        
        contentArea.classList.remove('active');
        
        setTimeout(() => {
            if (sectionData && sectionData[currentLang]) {
                contentArea.innerHTML = sectionData[currentLang];
            } else {
                contentArea.innerHTML = currentLang === 'es' 
                    ? '<h2>Contenido no encontrado</h2>'
                    : '<h2>Content not found</h2>';
            }
            contentArea.classList.add('active');
        }, 300); // Wait for transition
    }

    // --- Actualizar UI (i18n) ---
    function updateUI() {
        // Actualizar textos estáticos
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (window.WikiTranslations[currentLang][key]) {
                el.textContent = window.WikiTranslations[currentLang][key];
            }
        });
    }
});
