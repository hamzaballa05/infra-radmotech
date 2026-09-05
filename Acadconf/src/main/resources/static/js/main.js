// File upload preview
document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('fichierInput');
    const uploadZone = document.getElementById('uploadZone');
    const fileInfo = document.getElementById('fileInfo');

    if (uploadZone && fileInput) {
        uploadZone.addEventListener('click', () => fileInput.click());

        uploadZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadZone.classList.add('dragover');
        });

        uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('dragover'));

        uploadZone.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadZone.classList.remove('dragover');
            fileInput.files = e.dataTransfer.files;
            updateFileInfo(fileInput.files[0]);
        });

        fileInput.addEventListener('change', () => updateFileInfo(fileInput.files[0]));
    }

    function updateFileInfo(file) {
        if (!file || !fileInfo) return;
        const sizeKb = (file.size / 1024).toFixed(1);
        const sizeMb = (file.size / 1024 / 1024).toFixed(2);
        const display = file.size > 1024 * 1024 ? sizeMb + ' Mo' : sizeKb + ' Ko';
        fileInfo.innerHTML = `<i class="bi bi-file-earmark-pdf text-danger me-2"></i><strong>${file.name}</strong> <span class="text-muted">(${display})</span>`;
        fileInfo.classList.remove('d-none');
    }

    // Auto-dismiss alerts after 5s
    document.querySelectorAll('.alert').forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });
});
