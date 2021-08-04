$(document).ready(function () {
    $("#import-hosts-to-csv-form").on("submit", function (e) {
        // cancel the default behavior
        e?.preventDefault();
        uploadCsv()
    });
});

function uploadCsv() {
    let form = $('#import-hosts-to-csv-form')[0];
    let data = new FormData(form)
    data.append(_csrf_param_name, _csrf_token)

    $("#btnSubmit").prop("disabled", true);

    $.ajax({
        url: "/hosts/import",
        type: "POST",
        enctype: 'multipart/form-data',
        data: data,
        processData: false, //prevent jQuery from automatically transforming the data into a query string
        contentType: false,
        cache: false,
        success: function (res) {
            console.log(res);
            alert('Import completed!')
            $("#btnSubmit").prop("disabled", false);
            location.reload();
        },
        error: function (err) {
            console.log(err.responseText);
            $("#error").show().text(err.responseText);
            $("#btnSubmit").prop("disabled", false);
            setTimeout(function() {
                $('#error').hide()
            }, 4000);
        }
    });
}