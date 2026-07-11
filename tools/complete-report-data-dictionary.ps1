param(
    [Parameter(Mandatory = $true)]
    [string]$SourcePath,

    [Parameter(Mandatory = $true)]
    [string]$DestinationPath
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Set-CellText {
    param(
        [Parameter(Mandatory = $true)]
        [System.Xml.XmlNode]$Cell,

        [Parameter(Mandatory = $true)]
        [string]$Value,

        [Parameter(Mandatory = $true)]
        [xml]$DocumentXml,

        [Parameter(Mandatory = $true)]
        [System.Xml.XmlNamespaceManager]$NamespaceManager
    )

    $textNodes = @($Cell.SelectNodes('.//w:t', $NamespaceManager))
    if ($textNodes.Count -gt 0) {
        $textNodes[0].InnerText = $Value
        foreach ($textNode in ($textNodes | Select-Object -Skip 1)) {
            $textNode.InnerText = ''
        }
        return
    }

    $wordNamespace = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
    $paragraph = $Cell.SelectSingleNode('./w:p', $NamespaceManager)
    if ($null -eq $paragraph) {
        $paragraph = $DocumentXml.CreateElement('w', 'p', $wordNamespace)
        [void]$Cell.AppendChild($paragraph)
    }

    $run = $DocumentXml.CreateElement('w', 'r', $wordNamespace)
    $textNode = $DocumentXml.CreateElement('w', 't', $wordNamespace)
    $textNode.InnerText = $Value
    [void]$run.AppendChild($textNode)
    [void]$paragraph.AppendChild($run)
}

function Set-DataTableRows {
    param(
        [Parameter(Mandatory = $true)]
        [System.Xml.XmlNode]$Table,

        [Parameter(Mandatory = $true)]
        [object[]]$Rows,

        [Parameter(Mandatory = $true)]
        [xml]$DocumentXml,

        [Parameter(Mandatory = $true)]
        [System.Xml.XmlNamespaceManager]$NamespaceManager
    )

    $tableRows = @($Table.SelectNodes('./w:tr', $NamespaceManager))
    $dataStartIndex = 3
    if (($tableRows.Count - $dataStartIndex) -ne $Rows.Count) {
        throw "Unexpected data row count. Expected $($Rows.Count), found $($tableRows.Count - $dataStartIndex)."
    }

    for ($rowIndex = 0; $rowIndex -lt $Rows.Count; $rowIndex++) {
        $cells = @($tableRows[$rowIndex + $dataStartIndex].SelectNodes('./w:tc', $NamespaceManager))
        if ($cells.Count -ne 6) {
            throw "Unexpected cell count in data row $($rowIndex + 1): $($cells.Count)."
        }

        for ($columnIndex = 0; $columnIndex -lt 6; $columnIndex++) {
            Set-CellText -Cell $cells[$columnIndex] -Value $Rows[$rowIndex][$columnIndex] -DocumentXml $DocumentXml -NamespaceManager $NamespaceManager
        }
    }
}

$tableDefinitions = @{
    3 = @(
        @('id', 'BIGINT', '无', '主键', '无', '用户ID'),
        @('username', 'VARCHAR', '50', '无', '无', '登录用户名'),
        @('password', 'VARCHAR', '255', '无', '无', 'BCrypt加密密码'),
        @('email', 'VARCHAR', '100', '无', '无', '邮箱'),
        @('nickname', 'VARCHAR', '80', '无', '无', '昵称'),
        @('avatar', 'VARCHAR', '255', '无', '无', '头像地址'),
        @('profile_background', 'VARCHAR', '255', '无', '无', '个人主页背景图地址'),
        @('status', 'TINYINT', '1', '无', '无', '账号状态：1正常，0禁用'),
        @('created_at', 'DATETIME', '无', '无', '无', '创建时间'),
        @('updated_at', 'DATETIME', '无', '无', '无', '更新时间')
    )
    4 = @(
        @('id', 'BIGINT', '无', '主键', '无', '资料ID'),
        @('user_id', 'BIGINT', '无', '逻辑外键', 'user(id)', '所属用户'),
        @('name', 'VARCHAR', '180', '无', '无', '资料名称'),
        @('type', 'VARCHAR', '40', '无', '无', '资料类型'),
        @('file_url', 'VARCHAR', '500', '无', '无', '文件访问地址'),
        @('file_size', 'BIGINT', '无', '无', '无', '文件大小（字节）'),
        @('favorite', 'TINYINT', '1', '无', '无', '是否收藏：1是，0否'),
        @('progress_percent', 'INT', '无', '无', '无', '学习进度百分比'),
        @('annotations_json', 'TEXT', '不定长', '无', '无', '批注与截图卡片信息'),
        @('last_studied_at', 'TIMESTAMP', '无', '无', '无', '最近学习时间')
    )
    5 = @(
        @('id', 'BIGINT', '无', '主键', '无', '题目ID'),
        @('content', 'TEXT', '不定长', '无', '无', '题干内容'),
        @('question_type', 'VARCHAR', '30', '无', '无', '题型'),
        @('options_json', 'JSON', '不定长', '无', '无', '选项JSON'),
        @('correct_answer', 'VARCHAR', '1000', '无', '无', '标准答案'),
        @('analysis', 'TEXT', '不定长', '无', '无', '答案解析'),
        @('difficulty', 'TINYINT', '无', '无', '无', '难度等级'),
        @('category_id', 'BIGINT', '无', '外键', 'category(id)', '所属分类'),
        @('source_resource_id', 'BIGINT', '无', '逻辑外键', 'learning_resource(id)', '来源资料'),
        @('created_by', 'BIGINT', '无', '外键', 'user(id)', '创建者')
    )
    6 = @(
        @('id', 'BIGINT', '无', '主键', '无', '分类ID'),
        @('name', 'VARCHAR', '80', '无', '无', '分类名称'),
        @('description', 'VARCHAR', '255', '无', '无', '分类说明'),
        @('parent_id', 'BIGINT', '无', '外键', 'category(id)', '父分类'),
        @('active', 'TINYINT', '1', '无', '无', '是否启用：1是，0否'),
        @('sort_order', 'INT', '无', '无', '无', '排序号'),
        @('created_by', 'BIGINT', '无', '外键', 'user(id)', '创建者'),
        @('created_at', 'DATETIME', '无', '无', '无', '创建时间'),
        @('updated_at', 'DATETIME', '无', '无', '无', '更新时间')
    )
    7 = @(
        @('id', 'BIGINT', '无', '主键', '无', '记录ID'),
        @('user_id', 'BIGINT', '无', '外键', 'user(id)', '答题用户'),
        @('question_id', 'BIGINT', '无', '外键', 'question(id)', '题目ID'),
        @('user_answer', 'VARCHAR', '1000', '无', '无', '用户答案'),
        @('is_correct', 'TINYINT', '1', '无', '无', '是否正确：1正确，0错误'),
        @('practice_mode', 'VARCHAR', '30', '无', '无', '练习模式'),
        @('answered_at', 'DATETIME', '无', '无', '无', '答题时间')
    )
    8 = @(
        @('id', 'BIGINT', '无', '主键', '无', '错题记录ID'),
        @('user_id', 'BIGINT', '无', '外键', 'user(id)', '所属用户'),
        @('question_id', 'BIGINT', '无', '外键', 'question(id)', '题目ID'),
        @('wrong_count', 'INT', '无', '无', '无', '错误次数'),
        @('mastered', 'TINYINT', '1', '无', '无', '是否掌握：1已掌握，0未掌握'),
        @('last_wrong_at', 'DATETIME', '无', '无', '无', '最近答错时间'),
        @('last_reviewed_at', 'DATETIME', '无', '无', '无', '最近复习时间')
    )
    9 = @(
        @('id', 'BIGINT', '无', '主键', '无', '计划ID'),
        @('user_id', 'BIGINT', '无', '外键', 'user(id)', '所属用户'),
        @('title', 'VARCHAR', '150', '无', '无', '计划标题'),
        @('content', 'TEXT', '不定长', '无', '无', '计划内容'),
        @('plan_date', 'DATE', '无', '无', '无', '计划日期'),
        @('target_type', 'VARCHAR', '40', '无', '无', '关联对象类型'),
        @('target_id', 'BIGINT', '无', '逻辑关联', '由target_type决定', '关联对象ID'),
        @('status', 'VARCHAR', '30', '无', '无', '计划状态：pending/completed/cancelled'),
        @('completed_at', 'DATETIME', '无', '无', '无', '完成时间')
    )
    10 = @(
        @('id', 'BIGINT', '无', '主键', '无', '分享资源ID'),
        @('owner_id', 'BIGINT', '无', '逻辑外键', 'user(id)', '上传者'),
        @('title', 'VARCHAR', '120', '无', '无', '标题'),
        @('category', 'VARCHAR', '40', '无', '无', '资源分类'),
        @('kind', 'VARCHAR', '40', '无', '无', '资源形态'),
        @('file_url', 'VARCHAR', '500', '无', '无', '文件地址'),
        @('cover_url', 'VARCHAR', '500', '无', '无', '视频封面或封面图'),
        @('view_count', 'INT', '无', '无', '无', '浏览数'),
        @('like_count', 'INT', '无', '无', '无', '收藏或喜欢数')
    )
    11 = @(
        @('id', 'BIGINT', '无', '主键', '无', '主题ID'),
        @('user_id', 'BIGINT', '无', '逻辑外键', 'user(id)', '发帖用户'),
        @('board_id', 'VARCHAR', '40', '无', '无', '板块标识'),
        @('title', 'VARCHAR', '120', '无', '无', '主题标题'),
        @('content', 'TEXT', '不定长', '无', '无', '主题内容'),
        @('tags', 'VARCHAR', '300', '无', '无', '主题标签'),
        @('view_count', 'INT', '无', '无', '无', '浏览数'),
        @('like_count', 'INT', '无', '无', '无', '点赞数'),
        @('reply_count', 'INT', '无', '无', '无', '回复数')
    )
    12 = @(
        @('id', 'BIGINT', '无', '主键', '无', '回复ID'),
        @('thread_id', 'BIGINT', '无', '逻辑外键', 'forum_thread(id)', '所属主题'),
        @('user_id', 'BIGINT', '无', '逻辑外键', 'user(id)', '回复用户'),
        @('content', 'TEXT', '不定长', '无', '无', '回复内容'),
        @('created_at', 'DATETIME', '无', '无', '无', '回复时间')
    )
}

$resolvedSourcePath = (Resolve-Path -LiteralPath $SourcePath).Path
$destinationDirectory = Split-Path -Parent $DestinationPath
if (-not (Test-Path -LiteralPath $destinationDirectory)) {
    New-Item -ItemType Directory -LiteralPath $destinationDirectory -Force | Out-Null
}

Copy-Item -LiteralPath $resolvedSourcePath -Destination $DestinationPath -Force
$archive = [System.IO.Compression.ZipFile]::Open($DestinationPath, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $documentEntry = $archive.GetEntry('word/document.xml')
    if ($null -eq $documentEntry) {
        throw 'word/document.xml is missing from the DOCX package.'
    }

    $reader = [System.IO.StreamReader]::new($documentEntry.Open(), [System.Text.Encoding]::UTF8)
    try {
        [xml]$documentXml = $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }

    $namespaceManager = [System.Xml.XmlNamespaceManager]::new($documentXml.NameTable)
    $namespaceManager.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')
    $tables = @($documentXml.SelectNodes('//w:tbl', $namespaceManager))

    foreach ($tableIndex in $tableDefinitions.Keys) {
        if ($tables.Count -lt $tableIndex) {
            throw "Document does not contain table $tableIndex."
        }
        Set-DataTableRows -Table $tables[$tableIndex - 1] -Rows $tableDefinitions[$tableIndex] -DocumentXml $documentXml -NamespaceManager $namespaceManager
    }

    $documentEntry.Delete()
    $newEntry = $archive.CreateEntry('word/document.xml')
    $writer = [System.IO.StreamWriter]::new($newEntry.Open(), [System.Text.UTF8Encoding]::new($false))
    try {
        $documentXml.Save($writer)
    }
    finally {
        $writer.Dispose()
    }
}
finally {
    $archive.Dispose()
}

Write-Output "Created: $DestinationPath"
