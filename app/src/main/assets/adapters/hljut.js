/*
 * 黑龙江工程学院 · 正方教务（jwglxt 新版）课表适配脚本
 * 目标页面：kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151
 *
 * 解析规则要点：
 *  1) 逐行累积「当前节次」—— rowspan 会让同一节次跨多个 tr，绝不能按 td 下标取
 *  2) 红色斜体 = 待筛选，直接丢弃
 *  3) 字段用「下一个字段名」做边界，避免地点里的空格把内容截断
 *  4) 星期用 xq_N 的下标，不解析中文
 */
(function () {
    var BRIDGE = window.kcbBridge || null;
    function toast(m) { if (BRIDGE) BRIDGE.showToast(String(m)); }
    function reportError(m) { if (BRIDGE) BRIDGE.reportError(String(m)); }

    try {
        var table = document.getElementById('kblist_table');
        if (!table) {
            reportError('没有找到课表表格。请先登录并进入「个人课表查询」页面，等课表显示出来再点解析。');
            return;
        }

        var LABELS = ['周数', '校区', '上课地点', '教师', '教学班组成', '考核方式',
                      '课程学时组成', '学分', '课程性质', '课程总学时'];
        var STOPS = LABELS.join('|');

        function field(text, label) {
            var re = new RegExp(label + '\\s*[：:]\\s*([\\s\\S]*?)(?=\\s*(?:' + STOPS + ')\\s*[：:]|$)');
            var m = text.match(re);
            return m ? m[1].replace(/\s+/g, ' ').trim() : '';
        }

        function parseWeeks(text) {
            var out = [];
            var m = text.match(/周数\s*[：:]\s*([\d,\-\s]+)(周)?\s*(\(?[单双]\)?)?/);
            if (!m) return out;
            var seg = m[1].replace(/周/g, '');
            var only = m[3] || '';
            var isOdd = only.indexOf('单') >= 0;
            var isEven = only.indexOf('双') >= 0;
            seg.split(',').forEach(function (p) {
                p = p.trim();
                var mm = p.match(/^(\d+)\s*-\s*(\d+)$/);
                var a, b;
                if (mm) { a = parseInt(mm[1], 10); b = parseInt(mm[2], 10); }
                else {
                    var n = parseInt(p, 10);
                    if (isNaN(n)) return;
                    a = b = n;
                }
                for (var i = a; i <= b; i++) {
                    if (isOdd && i % 2 === 0) continue;
                    if (isEven && i % 2 === 1) continue;
                    out.push(i);
                }
            });
            return out;
        }

        function isPendingSelection(con) {
            var t = con.querySelector('.title');
            if (!t) return false;
            var fonts = t.querySelectorAll('font');
            for (var i = 0; i < fonts.length; i++) {
                var f = fonts[i];
                var color = (f.getAttribute('color') || '').toLowerCase();
                var style = (f.getAttribute('style') || '').toLowerCase();
                if (color === 'red' || style.indexOf('red') >= 0) return true;
                if (f.querySelector('i')) return true;
            }
            return false;
        }

        var courses = [];
        var maxWeek = 0;

        for (var day = 1; day <= 7; day++) {
            var tbody = document.getElementById('xq_' + day);
            if (!tbody) continue;

            var current = null;
            var rows = tbody.querySelectorAll('tr');

            for (var r = 0; r < rows.length; r++) {
                var row = rows[r];

                var fest = row.querySelector('.festival');
                if (fest) {
                    var ft = (fest.textContent || '').trim();
                    var fm = ft.match(/^(\d+)\s*-\s*(\d+)$/);
                    if (fm) current = { start: parseInt(fm[1], 10), end: parseInt(fm[2], 10) };
                    else {
                        var fs = ft.match(/^(\d+)$/);
                        if (fs) current = { start: parseInt(fs[1], 10), end: parseInt(fs[1], 10) };
                    }
                }

                var cons = row.querySelectorAll('.timetable_con');
                for (var c = 0; c < cons.length; c++) {
                    var con = cons[c];
                    if (isPendingSelection(con)) continue;

                    var titleEl = con.querySelector('.title');
                    var name = titleEl ? (titleEl.textContent || '').trim() : '';
                    if (!name) continue;

                    var text = (con.textContent || '').replace(/\s+/g, ' ');
                    var weeks = parseWeeks(text);
                    for (var w = 0; w < weeks.length; w++) {
                        if (weeks[w] > maxWeek) maxWeek = weeks[w];
                    }

                    var credit = parseFloat(field(text, '学分'));
                    courses.push({
                        name: name,
                        teacher: field(text, '教师'),
                        position: field(text, '上课地点'),
                        campus: field(text, '校区'),
                        credit: isNaN(credit) ? 0 : credit,
                        nature: field(text, '课程性质'),
                        assessment: field(text, '考核方式'),
                        day: day,
                        startSection: current ? current.start : 0,
                        endSection: current ? current.end : 0,
                        weeks: weeks
                    });
                }
            }
        }

        var semEl = document.querySelector('.timetable_title h6');
        var semester = semEl ? (semEl.textContent || '').trim() : '';

        if (courses.length === 0) {
            reportError('页面里没有解析到任何课程。请确认课表已经显示出来（不是空白页）。');
            return;
        }

        if (BRIDGE) {
            BRIDGE.saveCourses(JSON.stringify({
                semester: semester,
                maxWeek: maxWeek,
                courses: courses
            }));
            BRIDGE.notifyTaskCompletion();
        } else {
            toast('桥接对象未注入');
        }
    } catch (e) {
        reportError('解析出错：' + (e && e.message ? e.message : e));
    }
})();
