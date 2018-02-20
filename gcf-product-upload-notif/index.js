/**
 * Generic background Cloud Function to be triggered by Cloud Storage.
 *
 * @param {object} event The Cloud Functions event.
 * @param {function} callback The callback function.
 */
const {google} = require('googleapis');
const {auth} = require('google-auth-library');

exports.newProduct = function (event, callback) {
    const file = event.data;
    console.log("Starting function. Input: " + JSON.stringify(event))
    console.log("Getting Application credentials")
    console.log(auth.getApplicationDefault)
    auth.getApplicationDefault(function (err, authClient, projectId) {
        if (err) {
            throw err;
        }
        if (authClient.createScopedRequired && authClient.createScopedRequired()) {
            authClient = authClient.createScoped([
                'https://www.googleapis.com/auth/cloud-platform',
                'https://www.googleapis.com/auth/userinfo.email'
            ]);
        }
        if (!projectId) {
            projectId = 'rmp-sandbox'
        }
        console.log('Project ID:' + projectId)

        const dataflow = google.dataflow({
            version: 'v1b3',
            auth: authClient
        });
        dataflow.projects.templates.create({
            projectId: projectId,
            resource: {
                parameters: {
                    inputFile: `gs://${file.bucket}/${file.name}`,
                },
                jobName: 'cloud-fn-dataflow-test',
                gcsPath: 'gs://rmp-sandbox-df/templates/WordCount'
            }
        }, function (err, response) {
            if (err) {
                console.error("problem running dataflow template, error was: ", err);
            }
            console.log("Dataflow template response: ", response);
            callback();
        });
    });
};