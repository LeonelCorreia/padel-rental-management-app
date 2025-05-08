# Exercício 2
        Apesar de ser possível e funcional, pois, assumindo que id de rental é único, a única informação necessária
    para remover um rental é o seu id, que também está especificado no URL do pedido GET. Apesar disto não é o método
    recomendado, pois um delete a  um elemento da base de dados tem side effects, sendo idenpotente, mas não safe,
    sendo que um pedido get é ambos, criando uma inconsistência e uma não concordância com as especificações definidas
    no protocolo HTTP.