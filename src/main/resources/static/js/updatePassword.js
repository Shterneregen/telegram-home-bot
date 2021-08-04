$(document).ready(function () {
    $('form').submit(function(event) {
        savePass(event);
    });

    $(":password").keyup(function() {
        if($("#password").val() != $("#matchPassword").val()) {
            $("#globalError").show().html(passwordMatches);
        } else {
            $("#globalError").html("").hide();
        }
    });

    options = {
        common: {minChar:8},
        ui: {
            showVerdictsInsideProgressBar:true,
            showErrors:true,
            errorMessages:{
                  wordLength: wordLength,
                  wordSequences: wordSequences,
                  wordLowercase: wordLowercase,
                  wordUppercase: wordUppercase,
                  wordOneNumber: wordOneNumber,
                  wordOneSpecialChar: wordOneSpecialChar
                }
            }
        };
     $('#password').pwstrength(options);
});

function savePass(event){
    event?.preventDefault();
    $(".alert").html("").hide();
    $(".error-list").html("");
    if ($("#password").val() != $("#matchPassword").val()) {
        $("#globalError").show().html(passwordMatches);
        return;
    }

    let formData = {
                      oldPassword: $("#oldpass").val(),
                      newPassword: $("#password").val(),
                      [_csrf_param_name]: _csrf_token
                   };
    $.post(serverContext + "user/updatePassword", formData, function(data) {
        $("#successMessage").show().html(data.message);
        $("#oldpass").val("");
        $("#password").val("");
        $("#matchPassword").val("");

        setTimeout(function() {
            $('#successMessage').hide()
        }, 4000);
    })
    .fail(function(data) {
        if (data.responseJSON.error.indexOf("InvalidOldPassword") > -1) {
            $("#errormsg").show().append(data.responseJSON.message);
        } else if (data.responseJSON.error.indexOf("InternalError") > -1) {
            $("#errormsg").show().append(data.responseJSON.message);
        } else {
            let errors = $.parseJSON(data.responseJSON.message);
            $.each( errors, function( index,item ) {
                $("#globalError").show().html(item.defaultMessage);
            });
            errors = $.parseJSON(data.responseJSON.error);
            $.each( errors, function( index,item ) {
                $("#globalError").show().append(item.defaultMessage+"<br/>");
            });
        }
        setTimeout(function() {
            $('#errormsg').hide()
            $('#globalError').hide()
        }, 4000);
    });
}