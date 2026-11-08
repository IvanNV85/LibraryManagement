// JS riêng của UC11-15. Chỉ là phần "nâng cấp": tắt JS đi thì mọi form vẫn gửi bình thường.
//   1. form[data-confirm]        hỏi xác nhận trước khi gửi (hủy yêu cầu, hủy đặt trước)
//   2. form[data-once]           chống bấm đúp khi gửi yêu cầu mượn / đặt trước / gia hạn
//   3. select[data-autosubmit]   đổi lựa chọn là gửi form luôn (bộ lọc trạng thái)
//   4. .loan-due-chip[data-due]  hiện "Còn N ngày" / "Quá N ngày" cạnh hạn trả
//   5. form.js-mark-read         đánh dấu thông báo đã đọc không cần tải lại trang
(function () {
    'use strict';

    // ---------- 1, 2, 5: xử lý chung ở sự kiện submit ----------
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }

        if (form.classList.contains('js-mark-read')) {
            event.preventDefault();
            markRead(form);
            return;
        }

        if (form.hasAttribute('data-once')) {
            lockForm(form);
        }
    });

    function lockForm(form) {
        var buttons = form.querySelectorAll('button[type="submit"], button:not([type])');
        buttons.forEach(function (btn) {
            btn.classList.add('is-submitting');
            btn.setAttribute('aria-busy', 'true');
            // disable sau khi trình duyệt đã thu thập dữ liệu form
            setTimeout(function () { btn.disabled = true; }, 0);
            if (!btn.querySelector('.spinner-border')) {
                var spinner = document.createElement('span');
                spinner.className = 'spinner-border spinner-border-sm ms-2';
                spinner.setAttribute('role', 'status');
                spinner.setAttribute('aria-hidden', 'true');
                btn.appendChild(spinner);
            }
        });
    }

    // Quay lại bằng nút Back thì trang được khôi phục từ bộ nhớ đệm: mở khóa lại các nút
    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) {
            return;
        }
        document.querySelectorAll('form[data-once] button').forEach(function (btn) {
            btn.disabled = false;
            btn.classList.remove('is-submitting');
            btn.removeAttribute('aria-busy');
            var spinner = btn.querySelector('.spinner-border');
            if (spinner) {
                spinner.remove();
            }
        });
    });

    // ---------- 3: bộ lọc tự gửi ----------
    document.addEventListener('change', function (event) {
        var select = event.target;
        if (select instanceof HTMLSelectElement && select.hasAttribute('data-autosubmit') && select.form) {
            select.form.submit();
        }
    });

    // ---------- 4: đếm ngược hạn trả ----------
    function daysUntil(isoDate) {
        var p = isoDate.split('-');
        if (p.length !== 3) {
            return null;
        }
        var due = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
        var now = new Date();
        var today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        return Math.round((due - today) / 86400000);
    }

    function renderDueChips() {
        document.querySelectorAll('.loan-due-chip[data-due]').forEach(function (chip) {
            var days = daysUntil(chip.getAttribute('data-due'));
            if (days === null || isNaN(days)) {
                return;
            }
            chip.classList.remove('is-soon', 'is-late');
            if (days < 0) {
                chip.textContent = 'Quá hạn ' + (-days) + ' ngày';
                chip.classList.add('is-late');
            } else if (days === 0) {
                chip.textContent = 'Hạn trả là hôm nay';
                chip.classList.add('is-soon');
            } else {
                chip.textContent = 'Còn ' + days + ' ngày';
                if (days <= 2) {
                    chip.classList.add('is-soon');
                }
            }
        });
    }

    // ---------- 5: đánh dấu đã đọc bằng fetch ----------
    function markRead(form) {
        var button = form.querySelector('button');
        if (button) {
            button.disabled = true;
        }
        fetch(form.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(form)),
            credentials: 'same-origin',
            redirect: 'manual' // server trả redirect về danh sách; không cần tải lại cả trang
        }).then(function (response) {
            // redirect: 'manual' cho ra response kiểu "opaqueredirect" khi server chuyển hướng thành công
            if (response.type === 'opaqueredirect' || response.ok) {
                var item = form.closest('.notice-item');
                if (item) {
                    item.classList.remove('is-unread');
                }
                form.remove();
                changeUnreadBadge(-1);
            } else {
                fallbackSubmit(form);
            }
        }).catch(function () {
            fallbackSubmit(form);
        });
    }

    // Lỗi mạng hoặc server: gửi form theo cách thường (bỏ qua listener ở trên)
    function fallbackSubmit(form) {
        HTMLFormElement.prototype.submit.call(form);
    }

    // Cập nhật chấm đỏ trên menu (.js-unread-badge) và nút "Đánh dấu tất cả" (.js-read-all)
    function changeUnreadBadge(delta) {
        var remaining = null;
        document.querySelectorAll('.js-unread-badge').forEach(function (badge) {
            var current = parseInt(badge.textContent, 10);
            if (isNaN(current)) {
                return;
            }
            remaining = Math.max(0, current + delta);
            if (remaining === 0) {
                badge.remove();
            } else {
                badge.textContent = remaining;
            }
        });
        if (remaining === 0) {
            document.querySelectorAll('.js-read-all').forEach(function (el) { el.remove(); });
        }
    }

    function init() {
        renderDueChips();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
