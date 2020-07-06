job("Job1") {
	description()
	keepDependencies(false)
	scm {
		git {
			remote {
				github("shank2512/devops2", "https")
			}
			branch("*/master")
		}
	}
	disabled(false)
	triggers {
		scm("* * * * *") {
			ignorePostCommitHooks(false)
		}
	}
	concurrentBuild(false)
	steps {
		shell("""if ls | grep php
then
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mydeploy
  labels:
    app: web
spec:
  replicas: 1
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      name: myapp
      labels:
        app: web
        env: production
        region: IN
    spec:
      containers:
      - name: my-webserver
        image: httpd
        volumeMounts:
          - name: app-vol1
            mountPath: /var/www/html
      volumes:
      - name: app-vol1
        persistentVolumeClaim:
          claimName:  task-pv-claim
---
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  type: NodePort
  selector:
    app: web
  ports:
      # By default and for convenience, the `targetPort` is set to the same value as the `port` field.
    - port: 80
      targetPort: 80
      # Optional field
      # By default and for convenience, the Kubernetes control plane will allocate a port from a range (default: 30000-32767)
      nodePort: 30007
EOF
else
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mydeploy
  labels:
    app: web
spec:
  replicas: 1
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      name: myapp
      labels:
        app: web
        env: production
        region: IN
    spec:
      containers:
      - name: my-webserver
        image: httpd
        volumeMounts:
          - name: app-vol1
            mountPath: /usr/local/apache2/htdocs/
      volumes:
      - name: app-vol1
        persistentVolumeClaim:
          claimName:  task-pv-claim
---
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  type: NodePort
  selector:
    app: web
  ports:
      # By default and for convenience, the `targetPort` is set to the same value as the `port` field.
    - port: 80
      targetPort: 80
      # Optional field
      # By default and for convenience, the Kubernetes control plane will allocate a port from a range (default: 30000-32767)
      nodePort: 30007
EOF
fi""")
	}
}

job("Job2") {
	description()
	keepDependencies(false)
	disabled(false)
	concurrentBuild(false)
	triggers {
	        scm('@daily')
	        upstream {
	            upstreamProjects('Job1')
	            threshold('SUCCESS')
	        }
	    }
	steps {
		shell("""status=\$(curl -sL -w "%{http_code}" -I "http://192.168.99.105:30007" -o /dev/null)

if [[ \$status == 200 ]]
then
exit 0
else
exit 1
fi""")
	}
}
buildPipelineView('Task 6') {
    filterBuildQueue()
    filterExecutors()
    title('Project 6 Complete View')
    displayedBuilds(5)
    selectedJob('Job1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(60)
}

