/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.manager;

import static org.jboss.weld.logging.messages.BeanManagerMessage.NULL_DECLARING_BEAN;
import static org.jboss.weld.util.reflection.Reflections.cast;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Producer;

import org.jboss.weld.annotated.AnnotatedTypeValidator;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.bean.DisposalMethod;
import org.jboss.weld.bean.ProducerMethod;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.injection.producer.ProducerMethodProducer;
import org.jboss.weld.resources.MemberTransformer;

public class MethodProducerFactory<X> extends AbstractProducerFactory<X> {

    private final AnnotatedMethod<X> method;

    protected MethodProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean, BeanManagerImpl manager) {
        super(declaringBean, manager);
        this.method = cast(method);
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean) {
        if (getDeclaringBean() == null && !method.isStatic()) {
            throw new IllegalArgumentException(NULL_DECLARING_BEAN, method);
        }
        AnnotatedTypeValidator.validateAnnotatedMember(method);
        try {
            Producer<T> producer = createProducer(getDeclaringBean(), bean, null);
            getManager().getServices().get(InjectionTargetService.class).validateProducer(producer);
            return producer;
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Producers returned from this method are not validated. This is an optmization for {@link ProducerMethod} whose injection points are validated anyway. Internal use only.
     */
    public <T> Producer<T> createProducer(final Bean<X> declaringBean, final Bean<T> bean, DisposalMethod<X, T> disposalMethod) {
        EnhancedAnnotatedMethod<T, X> enhancedMethod = getManager().getServices().get(MemberTransformer.class).loadEnhancedMember(method, getManager().getId());
        return new ProducerMethodProducer<X, T>(enhancedMethod, disposalMethod) {

            @Override
            public AnnotatedMethod<X> getAnnotated() {
                return method;
            }

            @Override
            public BeanManagerImpl getBeanManager() {
                return getManager();
            }

            @Override
            public Bean<X> getDeclaringBean() {
                return declaringBean;
            }

            @Override
            public Bean<T> getBean() {
                return bean;
            }
        };
    }

}
