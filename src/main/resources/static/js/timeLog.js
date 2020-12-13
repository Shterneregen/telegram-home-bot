google.charts.load("current", {packages: ["timeline"]});
google.charts.setOnLoadCallback(drawChart);

function drawChart() {
    let container = document.getElementById('time_log_table');
    let chart = new google.visualization.Timeline(container);
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({type: 'string', id: 'Host'});
    dataTable.addColumn({type: 'string', id: 'State'});
    dataTable.addColumn({type: 'date', id: 'Start'});
    dataTable.addColumn({type: 'date', id: 'End'});

    Object.entries(timeLogMap).forEach((log) => {
        let deviceName = log[0]
        let logArray = log[1]
        for (let i = 0; i < logArray.length; i++) {
            let log = logArray[i]
            if (i === 0) {
                let state = log.state === 'FAILED' ? 'REACHABLE' : 'FAILED';
                dataTable.addRow([deviceName, state, new Date(0, 0, 0, 0, 0, 0), new Date(0, 0, 0, log.hours, log.minutes, 0)]);
            }
            if (i === logArray.length - 1) {
                dataTable.addRow([deviceName, log.state, new Date(0, 0, 0, log.hours, log.minutes, 0), new Date(0, 0, 0, 23, 59, 0)]);
            } else {
                let nextLog = logArray[i + 1]
                dataTable.addRow([deviceName, log.state, new Date(0, 0, 0, log.hours, log.minutes, 0), new Date(0, 0, 0, nextLog.hours, nextLog.minutes, 0)]);
            }
        }
    })

    let options = {
        timeline: {colorByRowLabel: false},
        backgroundColor: '#ffd'
    };
    chart.draw(dataTable, options);
}
