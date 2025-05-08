2. Não devemos fazer a deleção de um recurso num servidor através de um pedido GET
com uma parte do path do pedido a referir /delete, como é demonstrado no exemplo pois
o protocolo HTTP é um protocolo com operações bem definidas e o pedido GET não tem como 
funcionalidade a deleção de um recurso, mas sim pedir a represenação do mesmo. Para a 
deleção de um recurso devemos utilizar o pedido DELETE.