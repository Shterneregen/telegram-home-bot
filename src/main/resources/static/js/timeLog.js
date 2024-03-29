google.charts.load("current", {packages: ["timeline"]});
google.charts.setOnLoadCallback(drawChart);

function drawChart() {
    let timeLogTable = document.getElementById('time_log_table');
    let chart = new google.visualization.Timeline(timeLogTable);
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({type: 'string', id: 'Host'});
    dataTable.addColumn({type: 'string', id: 'State'});
    dataTable.addColumn({type: 'date', id: 'Start'});
    dataTable.addColumn({type: 'date', id: 'End'});

    timeLogMap = Object.entries(timeLogMap);
    if (timeLogMap.length === 0) {
        timeLogTable.innerHTML = '<h2>No time logs for this period</h2>';
        return;
    }

    let dates = Array.from(timeLogMap).map(([key, value]) => value).flat().map(log => new Date(log.createdDate))
    let minDate = new Date(Math.min(...dates));
    minDate.setHours(0, 0, 0, 0);
    let maxDate = new Date(Math.max(...dates));
    maxDate.setHours(23, 59, 59, 999);

    timeLogMap.forEach((log) => {
        let deviceName = log[0]
        let logArray = log[1]
        for (let i = 0; i < logArray.length; i++) {
            let log = logArray[i]
            if (i === 0) {
                let state = log.state === 'FAILED' ? 'REACHABLE' : 'FAILED';
                dataTable.addRow([deviceName, state, minDate, new Date(log.createdDate)]);
            }

            let endOfInterval;
            let isLast = i === logArray.length - 1;
            if (isLast) {
                let now = new Date()
                endOfInterval = maxDate > now ? now : maxDate
            } else {
                let nextLog = logArray[i + 1]
                endOfInterval = new Date(nextLog.createdDate)
            }
            dataTable.addRow([deviceName, log.state, new Date(log.createdDate), endOfInterval]);
        }
    });

    let colorMap = {
        FAILED: '#f36868',
        REACHABLE: '#82f582'
    }
    let colors = [];
    for (let i = 0; i < dataTable.getNumberOfRows(); i++) {
        colors.push(colorMap[dataTable.getValue(i, 1)]);
    }

    // TODO: find right way to calculate chartHeight
    // let rowHeight = 40;
    // let chartHeight = (dataTable.getNumberOfRows() + 1) * rowHeight;

    let options = {
        timeline: {colorByRowLabel: false},
        backgroundColor: '#ffd',
        avoidOverlappingGridLines: true,
        // height: 900,
        width: '100%',
        colors: colors
    };
    chart.draw(dataTable, options);
}
