MicroService Architeture Components
----------------------------------------
	Cloud Gateway
		-> Circuit Breaker
		-> Fallbacks
	MicroServices
		-> Department-Service
		-> User-Service
	Spring Cloud Server
		-> local git repository
	Eureka Service Registry
		-> All the MS will be resistered
				API-GATEWAY				
				CONFIG-SERVER			
				DEPARTMENT-SERVICE	
				HYSTRIX-DASHBOARD	
				USER-SERVICE				
	Hystrix Dashboard 
	Distributed Logging
		-> Zipkin server
		-> sleuth api libraries
    
    
Spring MicroServices Architecture
------------------------------------
End User
		-> Request1
		-> Request2

Components:
	Service Registry
		All components will be resgistered to Service Registry
		-> MicroService(s)
		-> API Gateway
		-> Hystrix Dashboard - Circuit breaker
			-> If service is down, we can have a callback message to the server

	API Gateway: -> All request goes through this
		/users/ -> UserService
		/departmenst/ -> DepartmentService

	Spring Cloud Server
		-> One place for all common configurations.
		-> git -> local repo

	Distributed Logging:
		-> Zipkin
		-> sleuth api -> trace-id, span-id,
			trace-id -> unique id for entire request
				spanid -> UserService
				spanid -> DepartmentService


Impelementation Details:
--------------------------

	DEPARTMENT-SERVICE -> 9001
	---------------------------------
	@SpringBootApplication
		-> entity: Department -> departmentId, departmentName, departmentAddress, departmentCode
		->repository:
				DepartmentRepository extends JpaRepository<Department, Long>
					Department findByDepartmentId(Long departmentId);
		-> service
				DepartmentService
						Department saveDepartment(Department department)
						Department findDepartmentById(Long departmentId)
		-> controller -> /departments
				DepartmentController
					"/" -> Department saveDepartment(@RequestBody Department department)
					"/{id}" -> Department findDepartmentById(@PathVariable("id") Long departmentId)

		Request Information: -> Save Department
			Method Type  : POST
			Request URL   : http://localhost:9001/departments/
			Request Body : { "departmentName":"IT", "departmentAddress": "3rd Cross, First Street", "departmentCode":"IR-006" }
			Response		: { "departmentName":"IT", "departmentAddress": "3rd Cross, First Street", "departmentCode":"IR-006" }

		Request Information: -> Get Department
			Method Type  : GET
			Request URL   : http://localhost:9001/departments/1
			Response  		: { "departmentName":"IT", "departmentAddress": "3rd Cross, First Street", "departmentCode":"IR-006" }

	Dependecy List:
		spring-boot-starter-web
		spring-boot-starter-data-jpa
		In Memory Database:
			h2
		lombok
		Service Resitry:
			spring-cloud-starter-netflix-eureka-client
		Spring Cloud Server
			spring-cloud-starter-config
		Distributed Logging:
			spring-cloud-starter-zipkin
			spring-cloud-starter-sleuth

	USER-SERVICE -> 9002
	-------------------------
	@SpringBootApplication
		-> entity: User -> userId, firstName, lastName, email, departmentId
		->repository:
				UserRepository extends JpaRepository<User, Long>
					User findByUserId(Long userId);
		-> service
				UserService
						User saveUser(User user)
						ResponseTemplateVO getUserWithDepartment(Long userId)
		-> controller -> /users
				UserController
					"/" -> User saveUser(@RequestBody User user)
					"/{id}" -> ResponseTemplateVO getUserWithDepartment(@PathVariable("id") Long userId)

		Request Information: -> Save User
			Method Type  : POST
			Request URL   : http://localhost:9002/users/
			Request Body :  { "firstName":"Raghavendra", "lastName": "M J", "email":"raghavendramj@gmail.com", "departmentId":"1" }
			Response 	    :  { "firstName":"Raghavendra", "lastName": "M J", "email":"raghavendramj@gmail.com", "departmentId":"1" }

		Request Information: -> Get User with Department
			Method Type  : GET
			Request URL   : http://localhost:9002/users/
			Response 	    :  { "user": { "userId": 1, "firstName": "Raghavendra", "lastName": "M J", "email": "raghavendramj@gmail.com", "departmentId": 1 }, "department": { "departmentId": 1, "departmentName": "IT", "departmentAddress": "3rd Cross, First Street", "departmentCode": "IR-006" } }

		Dependecy List:
			spring-boot-starter-web
			spring-boot-starter-data-jpa
			In Memory Database:
				h2
			lombok
			Service Resitry:
				spring-cloud-starter-netflix-eureka-client
			Spring Cloud Server
				spring-cloud-starter-config
			Distributed Logging:
				spring-cloud-starter-zipkin
				spring-cloud-starter-sleuth


	SERVICE-REGISTRY: ->  8761
	------------------------------
		@SpringBootApplication
		@EnableEurekaServer

		Spring Cloud NetFlix Eureka Server ::	http://localhost:8761/ : Instances currently registered with Eureka
		--------------------------------------------------------------------------------------------------------------
		Application						AMIs			Availability 	Zones		Status
		--------------------------------------------------------------------------------------------------------------
		API-GATEWAY				n/a 					(1)				(1)		UP (1) - host.docker.internal:API-GATEWAY:9191
		CONFIG-SERVER			n/a 					(1)				(1)		UP (1) - host.docker.internal:CONFIG-SERVER:9296
		DEPARTMENT-SERVICE	n/a 					(1)				(1)		UP (1) - host.docker.internal:DEPARTMENT-SERVICE:9001
		HYSTRIX-DASHBOARD	n/a 					(1)				(1)		UP (1) - host.docker.internal:HYSTRIX-DASHBOARD:9295
		USER-SERVICE				n/a 					(1)				(1)	UP (1) - host.docker.internal:USER-SERVICE:9002

		@Bean
		@LoadBalanced
		RestTemplate
			-> For multiple

		Dependecy List:
			spring-cloud-config-server
			spring-cloud-starter-netflix-eureka-client

	CLOUD-API-GATEWAY -> 9191
	---------------------------------
		@SpringBootApplication
		@EnableEurekaClient
		@EnableHystrix
			 routes:
				- id: USER-SERVICE
				  uri: lb://USER-SERVICE
				  predicates:
					- Path=/users/**
				- id: DEPARTMENT-SERVICE
				  uri: lb://DEPARTMENT-SERVICE
				  predicates:
					- Path=/departments/**

			Now all of out requests changes:
			----------------------------------
				User-Service: POST: http://localhost:9191/users/
				User-Service: GET: http://localhost:9191/users/1

				Department-Service: POST: http://localhost:9191/departments/
				Department-Service: GET: http://localhost:9191/departments/1

		For Circuit Breakers:
		---------------------
			Fallback Methods:
				FallBackMethodController:
					/userServiceFallBack 				->  String userServiceFallbackMethod()
					/departmentServiceFallBack  -> String departmentServiceFallbackMethod()
			  filters:
				- name: CircuitBreaker
				  args:
					name: USER-SERVICE
					fallbackuri: forward:/userServiceFallBack
			  filters:
				- name: CircuitBreaker
				  args:
					name: DEPARTMENT-SERVICE
					fallbackuri: forward:/departmentServiceFallBack

				To wait until we get the response, faiing which redirect to fallback
					hystrixcommandfallbackcmdexecutionisolationthreadtimeoutInMilliSeconds4000

		Dependecy List:
			spring-boot-starter-actuator
			spring-cloud-starter-netflix-hystrix
			spring-cloud-starter-gateway
			spring-cloud-starter-netflix-eureka-client
			spring-cloud-starter-config
			spring-cloud-starter-circuitbreaker-reactor-resilience4j


	HYSTRIX-DASHBOARD -> 9295
	---------------------------------
		@SpringBootApplication
		@EnableHystrixDashboard
		@EnableEurekaClient

		hystrix.dashboard.proxy-stream-allow-list. "*"

		Start the stream for monitoring:
			http://localhost:9191/actuator/hystrix.stream

		Monitor:
			http://localhost:9295/hystrix -> http://localhost:9191/actuator/hystrix.stream

		We can monitor all the access/failures etc..

		Dependecy List:
			spring-cloud-starter-config
			spring-cloud-starter-netflix-eureka-client
			spring-cloud-starter-netflix-hystrix-dashboard
