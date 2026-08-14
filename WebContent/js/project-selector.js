/* ============================================================================
 * 项目选择弹窗组件（单选）
 * 用法：
 *   1) RB.openProjectPicker({ coYear?, defaultGroupId?, keyword?, onSelect, onCancel })
 *      直接打开弹窗，选中后回调 onSelect(project)，project 含 id/projectName/coYear/brand/projectGroupId/projectGroupName 等
 *   2) RB.bindProjectSelector({ trigger: '#btnSelProj', display: '#txtProjName', hidden: '#projectId',
 *                                 hiddenName?: '#projectName', onChange?: fn, coYear?: fn|string })
 *      把一组元素绑定成"选择项目"控件：点 trigger 开弹窗，选中后把项目名写到 display，
 *      项目 id 写到 hidden（input hidden），项目名/年度也可额外写到其他 hiddenName 字段。
 *      coYear 可以是字符串或返回当前年度的函数（弹窗里按年度做默认过滤）
 * ============================================================================
 * 依赖：jquery, utils.js (RB.api/RB.post/RB.openModal/RB.closeModal/RB.toast)
 */
(function (RB) {
    var MODAL_ID = 'rb_project_picker_modal';
    var _groupsLoaded = false;
    var _groups = [];
    var _curSelection = null;
    var _curRows = [];

    function ensureModal() {
        if ($('#' + MODAL_ID).length) return;
        var html =
            '<div id="' + MODAL_ID + '" class="modal-mask">' +
            '  <div class="modal" style="min-width:760px;width:820px;">' +
            '    <div class="modal-header"><span>选择项目（单选）</span>' +
            '      <div class="spacer"></div><span class="close" onclick="RB.closeProjectPicker()">×</span>' +
            '    </div>' +
            '    <div class="modal-body">' +
            '      <div style="display:flex;gap:10px;align-items:center;margin-bottom:10px;">' +
            '        <select id="pp_groupId" style="min-width:160px;"><option value="">全部项目分组</option></select>' +
            '        <select id="pp_coYear" style="width:120px;"><option value="">全部年度</option></select>' +
            '        <input id="pp_keyword" placeholder="关键字：项目名/编号/厂牌" style="flex:1;">' +
            '        <button class="btn btn-primary" id="pp_btnSearch">搜索</button>' +
            '      </div>' +
            '      <div style="max-height:50vh;overflow:auto;border:1px solid #eee;border-radius:4px;">' +
            '        <table class="table-nowrap" style="width:100%;">' +
            '          <thead style="position:sticky;top:0;background:#fafafa;z-index:1;">' +
            '            <tr>' +
            '              <th style="width:40px;"></th>' +
            '              <th>编号</th><th>项目名称</th><th>年度</th><th>厂牌</th><th>合作品种</th><th>项目分组</th><th>状态</th>' +
            '            </tr>' +
            '          </thead>' +
            '          <tbody id="pp_tbody"></tbody>' +
            '        </table>' +
            '      </div>' +
            '      <div id="pp_emptyHint" style="text-align:center;color:#999;padding:24px;display:none;">未找到匹配的项目</div>' +
            '    </div>' +
            '    <div class="modal-footer">' +
            '      <span id="pp_curSelHint" style="float:left;color:#606266;margin-top:6px;"></span>' +
            '      <button class="btn" onclick="RB.closeProjectPicker()">取消</button> ' +
            '      <button class="btn btn-primary" onclick="RB._projectPickerConfirm()">确定</button>' +
            '    </div>' +
            '  </div>' +
            '</div>';
        $('body').append(html);

        $('#pp_btnSearch').on('click', function () { doSearch(); });
        $('#pp_keyword').on('keydown', function (e) { if (e.keyCode === 13) doSearch(); });
        $('#pp_groupId').on('change', function () { doSearch(); });
        $('#pp_coYear').on('change', function () { doSearch(); });
    }

    function loadDicts(opts) {
        $.when(
            _groupsLoaded ? $.Deferred().resolve() : RB.post('/api/project?op=groups').done(function (r) {
                if (r.code === 0) {
                    _groups = r.data || [];
                    _groupsLoaded = true;
                }
            }),
            RB.post('/api/project?op=listYears').done(function (r) {
                if (r.code === 0) {
                    var html = '<option value="">全部年度</option>';
                    for (var i = 0; i < (r.data || []).length; i++) html += '<option>' + r.data[i] + '</option>';
                    $('#pp_coYear').html(html);
                    if (opts && opts.coYear) {
                        var y = (typeof opts.coYear === 'function') ? opts.coYear() : opts.coYear;
                        if (y) $('#pp_coYear').val(y);
                    }
                }
            })
        ).done(function () {
            var h = '<option value="">全部项目分组</option>';
            for (var i = 0; i < _groups.length; i++) h += '<option value="' + _groups[i].id + '">' + _groups[i].name + '</option>';
            $('#pp_groupId').html(h);
            if (opts && opts.defaultGroupId) $('#pp_groupId').val(opts.defaultGroupId);
            doSearch();
        });
    }

    function doSearch() {
        var gid = $('#pp_groupId').val() || '';
        var coYear = $('#pp_coYear').val() || '';
        var kw = $.trim($('#pp_keyword').val());
        var param = {};
        if (gid) param.projectGroupId = gid;
        if (coYear) param.coYear = coYear;
        if (kw) param.keyword = kw;
        RB.post('/api/project?op=listFilters', param).done(function (r) {
            _curRows = [];
            _curSelection = null;
            $('#pp_curSelHint').text('');
            var html = '';
            if (r.code === 0 && r.data && r.data.length) {
                _curRows = r.data;
                var statusMap = {NEW:'立项中',APPROVING:'审批中',RUNNING:'执行中',FINISHED:'已完成',ARCHIVED:'已归档'};
                for (var i = 0; i < r.data.length; i++) {
                    var p = r.data[i];
                    html += '<tr data-idx="' + i + '" style="cursor:pointer;">' +
                        '<td><input type="radio" name="pp_sel" value="' + i + '"></td>' +
                        '<td>' + (p.projectCode || '') + '</td>' +
                        '<td>' + (p.projectName || '') + '</td>' +
                        '<td>' + (p.coYear || '') + '</td>' +
                        '<td>' + (p.brand || '') + '</td>' +
                        '<td>' + (p.coProduct || '') + '</td>' +
                        '<td>' + (p.projectGroupName || '') + '</td>' +
                        '<td>' + (statusMap[p.status] || p.status || '') + '</td>' +
                        '</tr>';
                }
                $('#pp_tbody').html(html);
                $('#pp_emptyHint').hide();
                $('#pp_tbody tr').on('click', function () {
                    var idx = $(this).attr('data-idx');
                    $(this).find('input[name="pp_sel"]').prop('checked', true);
                    _curSelection = _curRows[idx];
                    $('#pp_curSelHint').text('已选：' + _curSelection.projectName +
                        (_curSelection.coYear ? '（' + _curSelection.coYear + '）' : ''));
                });
                $('#pp_tbody tr input[name="pp_sel"]').on('click', function (e) { e.stopPropagation(); });
            } else {
                $('#pp_tbody').html('');
                $('#pp_emptyHint').show();
            }
        });
    }

    RB.openProjectPicker = function (opts) {
        opts = opts || {};
        ensureModal();
        _curSelection = null;
        $('#pp_keyword').val(opts.keyword || '');
        $('#pp_curSelHint').text('');
        $('#pp_tbody').html('');
        $('#pp_emptyHint').hide();
        RB.__projectPickerOnSelect = opts.onSelect;
        RB.__projectPickerOnCancel = opts.onCancel;
        loadDicts({ coYear: opts.coYear, defaultGroupId: opts.defaultGroupId });
        RB.openModal(MODAL_ID);
    };

    RB.closeProjectPicker = function () {
        RB.closeModal(MODAL_ID);
        if (typeof RB.__projectPickerOnCancel === 'function') {
            var fn = RB.__projectPickerOnCancel;
            RB.__projectPickerOnCancel = null;
            fn();
        }
    };

    RB._projectPickerConfirm = function () {
        if (!_curSelection) { RB.toast('请选择一个项目', 'error'); return; }
        RB.closeModal(MODAL_ID);
        var fn = RB.__projectPickerOnSelect;
        RB.__projectPickerOnSelect = null;
        RB.__projectPickerOnCancel = null;
        if (typeof fn === 'function') fn(_curSelection);
    };

    /**
     * 把一组 DOM 绑定为"选择项目"的控件：
     *   trigger      点击打开弹窗的选择器（按钮或任何元素）
     *   display      显示项目名（和分组）的选择器（input 或 span/div）
     *   hidden       存储项目 id 的隐藏 input 选择器
     *   hiddenName   可选，存储项目名称的隐藏 input 选择器
     *   hiddenYear   可选，存储项目年度的隐藏 input 选择器
     *   coYear       可选，string 或 返回当前年度的函数
     *   onChange     可选，选中后回调 function(project)
     */
    RB.bindProjectSelector = function (cfg) {
        if (!cfg || !cfg.trigger) return;
        $(document).on('click', cfg.trigger, function () {
            RB.openProjectPicker({
                coYear: cfg.coYear,
                onSelect: function (p) {
                    var label = p.projectName + (p.projectGroupName ? '【' + p.projectGroupName + '】' : '') + (p.coYear ? '（' + p.coYear + '）' : '');
                    if (cfg.display) $(cfg.display).val ? $(cfg.display).val(label) : $(cfg.display).text(label);
                    if (cfg.hidden) $(cfg.hidden).val(p.id).trigger('change');
                    if (cfg.hiddenName) $(cfg.hiddenName).val(p.projectName);
                    if (cfg.hiddenYear) $(cfg.hiddenYear).val(p.coYear || '');
                    if (typeof cfg.onChange === 'function') cfg.onChange(p);
                }
            });
        });
    };

})(window.RB = window.RB || {});
