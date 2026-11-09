// Gợi ý từ khóa khi gõ ô tìm kiếm: chờ 300ms sau lần gõ cuối rồi mới gọi server
(function () {
    var input = document.getElementById('keyword');
    var list = document.getElementById('suggestions');
    if (!input || !list) {
        return;
    }
    var timer = null;

    input.addEventListener('input', function () {
        clearTimeout(timer);
        var q = input.value.trim();
        if (q.length < 2) {
            list.innerHTML = '';
            return;
        }
        timer = setTimeout(function () {
            fetch(input.dataset.suggestUrl + '?q=' + encodeURIComponent(q))
                .then(function (res) { return res.text(); })
                .then(function (text) {
                    list.innerHTML = '';
                    text.split('\n').forEach(function (title) {
                        if (title) {
                            var option = document.createElement('option');
                            option.value = title;
                            list.appendChild(option);
                        }
                    });
                });
        }, 300);
    });
})();
