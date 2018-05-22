




function getErrorMessage(errorCode) {
    switch(errorCode) {
        case 'GLIST01':
            return 'Entry Error has occurred. Please Provide valid ID in URL.';
            break;
        case 'GLIST02':
            return 'Entry Error has occurred. You can not leave this empty.';
            break;
        case 'GLIST03':
            return 'Entry Error has occurred. Please Provide valid ID. Your Entry should be number.';
            break;
        case 'GLIST04':
            return 'Entry Error has occurred. Your Entry should be between lowest and highest Numbers.';
            break;
        case 'GLIST05':
            return 'Entry Error has occurred. Please Provide valid ID.';
            break;
        case 'LIBGLIST01':
            return 'Selection Error has occurred.'
                   'Please choose a different number of records per page.';
            break;
        case 'server_down':
            return 'sorry server is down';
            break;
    }

    return 'Unknown error.';
}
/**
 * Display Error message using alertify


 */
function displayError(message){
    alertify.alert('Error', message).set('modal', false);
}

function displayErrorByCode(errorCode) {
    displayError(getErrorMessage(errorCode));
}


/**
 * Js load for adding header and footer file into each page


 */
$(document).ready(function(){
    $( "#footer" ).load( "footer.html" );
    $( "#header" ).load( "header.html" );
});