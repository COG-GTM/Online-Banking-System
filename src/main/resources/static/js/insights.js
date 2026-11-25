(function () {
    'use strict';

    var data = window.INSIGHTS || {};
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var COLORS = ['#0b3d6e', '#337ab7', '#5cc1ea', '#1f5f9e', '#9fd6f2'];

    function money(value) {
        return '$' + Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function el(name, attrs, parent) {
        var node = document.createElementNS(SVG_NS, name);
        Object.keys(attrs || {}).forEach(function (key) { node.setAttribute(key, attrs[key]); });
        if (parent) { parent.appendChild(node); }
        return node;
    }

    function renderTrend() {
        var host = document.getElementById('trendChart');
        var tooltip = document.getElementById('trendTooltip');
        var points = data.trend || [];
        if (!host) { return; }
        if (points.length < 2) {
            host.innerHTML = '<p class="text-muted">Make a deposit to start your trend line.</p>';
            return;
        }

        var w = 640, h = 260, pad = { top: 16, right: 16, bottom: 28, left: 64 };
        var values = points.map(function (p) { return p.value; });
        var max = Math.max.apply(null, values), min = Math.min.apply(null, values.concat([0]));
        if (max === min) { max = min + 1; }
        var x = function (i) { return pad.left + i * (w - pad.left - pad.right) / (points.length - 1); };
        var y = function (v) { return pad.top + (max - v) * (h - pad.top - pad.bottom) / (max - min); };

        var svg = el('svg', { viewBox: '0 0 ' + w + ' ' + h, preserveAspectRatio: 'none', role: 'img', 'aria-label': 'Net worth trend' }, host);
        var defs = el('defs', {}, svg);
        var grad = el('linearGradient', { id: 'trendFill', x1: 0, y1: 0, x2: 0, y2: 1 }, defs);
        el('stop', { offset: '0%', 'stop-color': '#337ab7', 'stop-opacity': 0.35 }, grad);
        el('stop', { offset: '100%', 'stop-color': '#337ab7', 'stop-opacity': 0 }, grad);

        for (var g = 0; g <= 4; g++) {
            var gv = min + (max - min) * g / 4;
            el('line', { x1: pad.left, x2: w - pad.right, y1: y(gv), y2: y(gv), class: 'grid' }, svg);
            el('text', { x: pad.left - 8, y: y(gv) + 4, 'text-anchor': 'end', class: 'axis' }, svg).textContent = money(gv).replace(/\.\d\d$/, '');
        }

        var line = points.map(function (p, i) { return (i ? 'L' : 'M') + x(i) + ',' + y(p.value); }).join(' ');
        el('path', { d: line + ' L' + x(points.length - 1) + ',' + y(min) + ' L' + x(0) + ',' + y(min) + ' Z', fill: 'url(#trendFill)' }, svg);
        var path = el('path', { d: line, class: 'trend-line' }, svg);
        var length = path.getTotalLength ? path.getTotalLength() : 0;
        if (length) {
            path.style.strokeDasharray = length;
            path.style.strokeDashoffset = length;
            requestAnimationFrame(function () { path.style.strokeDashoffset = 0; });
        }

        points.forEach(function (p, i) {
            var dot = el('circle', { cx: x(i), cy: y(p.value), r: 5, class: 'trend-dot', tabindex: 0, 'data-index': i }, svg);
            var show = function () {
                tooltip.innerHTML = '<strong>' + money(p.value) + '</strong><br/>' + p.label;
                tooltip.style.left = (x(i) / w * 100) + '%';
                tooltip.style.top = (y(p.value) / h * host.clientHeight) + 'px';
                tooltip.className = 'chart-tooltip visible';
            };
            var hide = function () { tooltip.className = 'chart-tooltip'; };
            dot.addEventListener('mouseenter', show);
            dot.addEventListener('focus', show);
            dot.addEventListener('mouseleave', hide);
            dot.addEventListener('blur', hide);
        });
        el('text', { x: x(0), y: h - 8, class: 'axis' }, svg).textContent = points[0].label;
        el('text', { x: x(points.length - 1), y: h - 8, 'text-anchor': 'end', class: 'axis' }, svg).textContent = points[points.length - 1].label;
    }

    function renderDonut() {
        var host = document.getElementById('donutChart');
        var legend = document.getElementById('donutLegend');
        var slices = data.breakdown || [];
        if (!host) { return; }
        if (!slices.length) {
            host.innerHTML = '<p class="text-muted">No activity yet.</p>';
            return;
        }
        var total = slices.reduce(function (sum, s) { return sum + Number(s.amount); }, 0);
        var r = 70, c = 2 * Math.PI * r, offset = 0;
        var svg = el('svg', { viewBox: '0 0 200 200', role: 'img', 'aria-label': 'Activity breakdown' }, host);
        slices.forEach(function (s, i) {
            var len = Number(s.amount) / total * c;
            var arc = el('circle', {
                cx: 100, cy: 100, r: r, fill: 'none', stroke: COLORS[i % COLORS.length], 'stroke-width': 28,
                'stroke-dasharray': len + ' ' + (c - len), 'stroke-dashoffset': -offset, transform: 'rotate(-90 100 100)', class: 'donut-arc'
            }, svg);
            el('title', {}, arc).textContent = s.label + ': ' + money(s.amount);
            offset += len;

            var item = document.createElement('li');
            item.innerHTML = '<span class="swatch" style="background:' + COLORS[i % COLORS.length] + '"></span>' +
                '<span class="legend-label"></span><span class="legend-value">' + s.percent + '%</span>';
            item.querySelector('.legend-label').textContent = s.label;
            legend.appendChild(item);
        });
        el('text', { x: 100, y: 96, 'text-anchor': 'middle', class: 'donut-total' }, svg).textContent = money(total);
        el('text', { x: 100, y: 116, 'text-anchor': 'middle', class: 'donut-caption' }, svg).textContent = 'total moved';
    }

    function renderRecipientBars() {
        var bars = document.querySelectorAll('#topRecipients .bar-fill');
        setTimeout(function () {
            Array.prototype.forEach.call(bars, function (bar) { bar.style.width = (bar.getAttribute('data-width') || 0) + '%'; });
        }, 100);
    }

    function setupGoal() {
        var form = document.getElementById('goalForm');
        if (!form) { return; }
        var input = document.getElementById('goalInput');
        var bar = document.getElementById('goalBar');
        var message = document.getElementById('goalMessage');
        var key = 'obs.savingsGoal.' + window.INSIGHTS_USER;
        var savings = Number(data.savingsBalance || 0);

        function render() {
            var goal = Number(localStorage.getItem(key) || 0);
            if (!goal) {
                bar.style.width = '0%';
                bar.textContent = '0%';
                message.textContent = 'Set a target to track your savings balance against it.';
                message.className = 'goal-message text-muted';
                return;
            }
            input.value = goal;
            var pct = Math.min(100, Math.floor(savings / goal * 100));
            bar.style.width = pct + '%';
            bar.textContent = pct + '%';
            if (savings >= goal) {
                message.textContent = 'Goal reached! You have ' + money(savings) + ' saved against a ' + money(goal) + ' goal.';
                message.className = 'goal-message text-primary';
            } else {
                message.textContent = money(goal - savings) + ' to go: ' + money(savings) + ' of ' + money(goal) + ' saved.';
                message.className = 'goal-message';
            }
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var goal = Math.round(Number(input.value));
            if (goal > 0) { localStorage.setItem(key, goal); }
            render();
        });
        document.getElementById('goalClear').addEventListener('click', function () {
            localStorage.removeItem(key);
            input.value = '';
            render();
        });
        render();
    }

    renderTrend();
    renderDonut();
    renderRecipientBars();
    setupGoal();
})();
